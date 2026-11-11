package panelweb

import (
	"context"
	"errors"
	"io/fs"
	"net/http"

	"vault/internal/httpapi/routepolicy"
	"vault/internal/store/postgres"
)

type verifiedActorKey struct{}

func (p *Web) classifiedRoutes() *routepolicy.Router {
	mux := routepolicy.New(panelRoutePolicies, p.guardRoute)
	assets, _ := fs.Sub(files, "assets")
	mux.Handle("GET /painel/assets/", http.StripPrefix("/painel/assets/", http.FileServerFS(assets)))
	mux.HandleFunc("GET /painel/{$}", p.start)
	mux.HandleFunc("POST /painel/entrar", p.signIn)
	mux.HandleFunc("POST /painel/ativar", p.activate)
	mux.HandleFunc("POST /painel/sair", p.signOut)
	mux.HandleFunc("GET /painel/documentos/{id}", p.document)
	for section := range sections {
		mux.HandleFunc("GET /painel/"+section, func(w http.ResponseWriter, r *http.Request) {
			r.SetPathValue("section", section)
			p.section(w, r)
		})
	}
	for _, section := range []string{"usuarios", "equipe", "configuracoes", "perfil", "imoveis", "relatorios"} {
		mux.HandleFunc("POST /painel/"+section, func(w http.ResponseWriter, r *http.Request) {
			r.SetPathValue("section", section)
			p.change(w, r)
		})
	}
	return mux
}

func (p *Web) guardRoute(rule routepolicy.Rule, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		a, err := p.actor(r)
		if errors.Is(err, postgres.ErrNotFound) {
			if rule.Class == routepolicy.ReportAccess {
				http.NotFound(w, r)
			} else {
				p.redirect(w, r, "")
			}
			return
		}
		if err != nil {
			p.fail(w, err)
			return
		}
		// One database-verified actor per request; resource/RBAC checks still
		// run in services. No actor survives into the next request.
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), verifiedActorKey{}, a)))
	})
}
