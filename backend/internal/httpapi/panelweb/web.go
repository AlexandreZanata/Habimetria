package panelweb

import (
	"bytes"
	"embed"
	"errors"
	"fmt"
	"html/template"
	"log"
	"net/http"
	"net/url"
	"slices"
	"strconv"
	"strings"
	"time"

	"vault/internal/service/panel"
	"vault/internal/storage"
	"vault/internal/store/postgres"
)

//go:embed views/*.html assets
var files embed.FS

type Web struct {
	service        *panel.Service
	storage        storage.Reader
	origin, cookie string
	secure         bool
	views          *template.Template
}

func New(service *panel.Service, storage storage.Reader, origin string) (*Web, error) {
	parsed, err := url.Parse(origin)
	if err != nil || parsed.Hostname() == "" || parsed.User != nil || parsed.Path != "" || parsed.RawQuery != "" || parsed.Fragment != "" ||
		origin != parsed.Scheme+"://"+parsed.Host ||
		(parsed.Scheme != "https" && (parsed.Scheme != "http" || (parsed.Hostname() != "localhost" && parsed.Hostname() != "127.0.0.1" && parsed.Hostname() != "::1"))) {
		return nil, errors.New("PANEL_ORIGIN must be an HTTPS origin or local HTTP loopback")
	}
	view, err := template.New("panel").Funcs(template.FuncMap{
		"capabilities": roleCapabilities, "date": date, "role": roleLabel, "event": eventLabel, "has": slices.Contains[[]string, string],
	}).ParseFS(files, "views/*.html")
	if err != nil {
		return nil, fmt.Errorf("parse panel views: %w", err)
	}
	return &Web{service: service, storage: storage, origin: origin, cookie: cookieName(origin), secure: parsed.Scheme == "https", views: view}, nil
}

func (p *Web) Routes() http.Handler {
	mux := p.classifiedRoutes()
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'self'; script-src 'self'; font-src 'self'; img-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		// Keep the browser's Origin header on same-origin HTML form posts.
		w.Header().Set("Referrer-Policy", "same-origin")
		if r.Method == "POST" && r.Header.Get("Origin") != p.origin {
			http.Error(w, "Origem da solicitação inválida.", http.StatusForbidden)
			return
		}
		mux.ServeHTTP(w, r)
	})
}

func cookieName(origin string) string {
	if strings.HasPrefix(origin, "https://") {
		return "__Host-habimetria"
	}
	return "habimetria_local"
}

func sessionValue(r *http.Request, cookie string) string {
	value, err := r.Cookie(cookie)
	if err != nil {
		return ""
	}
	return value.Value
}

func (p *Web) actor(r *http.Request) (postgres.PanelActor, error) {
	if a, ok := r.Context().Value(verifiedActorKey{}).(postgres.PanelActor); ok {
		return a, nil
	}
	return p.service.Session(r.Context(), sessionValue(r, p.cookie))
}

func readForm(w http.ResponseWriter, r *http.Request) bool {
	r.Body = http.MaxBytesReader(w, r.Body, 16<<10)
	if err := r.ParseForm(); err != nil {
		http.Error(w, "Formulário inválido.", http.StatusBadRequest)
		return false
	}
	return true
}

func (p *Web) render(w http.ResponseWriter, status int, name string, data page) {
	var body bytes.Buffer
	if err := p.views.ExecuteTemplate(&body, name, data); err != nil {
		log.Printf("render panel %s: %v", name, err)
		http.Error(w, "Não foi possível carregar esta página.", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(status)
	_, _ = body.WriteTo(w)
}

func (p *Web) redirect(w http.ResponseWriter, r *http.Request, section string) {
	http.Redirect(w, r, "/painel/"+section, http.StatusSeeOther)
}

func (p *Web) fail(w http.ResponseWriter, err error) {
	status, message := issue(err)
	if status == http.StatusInternalServerError {
		log.Printf("panel request failed: %v", err)
	}
	p.render(w, status, "failure", page{Error: message})
}

func issue(err error) (int, string) {
	switch {
	case errors.Is(err, postgres.ErrPanelDenied):
		return 403, "Sua conta não tem permissão para esta ação."
	case errors.Is(err, postgres.ErrNotFound):
		return 404, "Registro indisponível para esta conta."
	case errors.Is(err, postgres.ErrPanelEmailTaken):
		return 409, "Este e-mail já está cadastrado."
	case errors.Is(err, postgres.ErrPanelPrimary):
		return 409, "A conta do proprietário principal está protegida."
	case errors.Is(err, panel.ErrInput):
		return 400, "Confira os dados informados."
	default:
		return 500, "Não foi possível concluir a operação. Tente novamente."
	}
}

func date(at time.Time) string { return at.UTC().Format("02/01/2006 15:04") + " UTC" }

func pageOffset(r *http.Request) int {
	number, err := strconv.Atoi(r.URL.Query().Get("pagina"))
	if err != nil || number < 1 || number > 10000 {
		return 0
	}
	return (number - 1) * 25
}
