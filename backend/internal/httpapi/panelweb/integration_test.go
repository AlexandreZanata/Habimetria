package panelweb_test

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"regexp"
	"strings"
	"testing"
	"time"

	"vault/internal/httpapi"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service"
	"vault/internal/service/panel"
	"vault/internal/storage/localstore"
	"vault/internal/store/postgres"
	"vault/migrations"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

const (
	testOrigin      = "http://localhost:8080"
	fixturePassword = "fixture password for integration"
)

type harness struct {
	t        *testing.T
	ctx      context.Context
	pool     *pgxpool.Pool
	panel    *panel.Service
	evidence *service.EvidenceService
	handler  http.Handler
}

func integration(t *testing.T) *harness {
	t.Helper()
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL unset")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 90*time.Second)
	t.Cleanup(cancel)
	admin, err := postgres.Open(ctx, dsn)
	require(t, err)
	t.Cleanup(admin.Close)
	name := "panel_" + strings.ReplaceAll(uuid.NewString(), "-", "")
	_, err = admin.Exec(ctx, "CREATE DATABASE "+name)
	require(t, err)
	t.Cleanup(func() {
		c, stop := context.WithTimeout(context.Background(), 10*time.Second)
		defer stop()
		if _, err := admin.Exec(c, "DROP DATABASE "+name+" WITH (FORCE)"); err != nil {
			t.Errorf("drop test database: %v", err)
		}
	})
	cfg := admin.Config()
	cfg.ConnConfig.Database = name
	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	require(t, err)
	t.Cleanup(pool.Close)
	require(t, postgres.Apply(ctx, pool, migrations.FS))
	p := panel.New(postgres.NewPanelStore(pool))
	store := localstore.New(t.TempDir())
	web, err := panelweb.New(p, store, testOrigin)
	require(t, err)
	evidence := service.NewEvidence(pool, store)
	router := httpapi.NewRouter(httpapi.Deps{
		Panel: web.Routes(), Guard: panelweb.ProtectLegacy(p, testOrigin, panelweb.LegacyPolicy{}),
		Room: service.NewRoom(pool), Evidence: evidence,
		Property: service.NewProperty(pool), Tenancy: service.NewTenancy(pool), Inspection: service.NewInspection(pool),
		Finding: service.NewFinding(pool), Coverage: service.NewCoverage(pool),
		Occurrence: service.NewOccurrence(pool), Compare: service.NewCompare(pool),
		Share: service.NewShare(pool, store, testOrigin, "integration-test-key"), HMACKey: "integration-test-key",
		Seal:   service.NewSeal(pool, "integration-test-key"),
		Report: service.NewReport(pool, store, testOrigin),
	})
	return &harness{t: t, ctx: ctx, pool: pool, panel: p, evidence: evidence, handler: httpapi.SecurityHeaders(router)}
}

func require(t *testing.T, err error) {
	t.Helper()
	if err != nil {
		t.Fatal(err)
	}
}

func (h *harness) call(method, path string, cookie *http.Cookie, values url.Values, status int) *httptest.ResponseRecorder {
	h.t.Helper()
	req := httptest.NewRequest(method, testOrigin+path, strings.NewReader(values.Encode())).WithContext(h.ctx)
	if method == "POST" {
		req.Header.Set("Origin", testOrigin)
		req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	}
	if cookie != nil {
		req.AddCookie(cookie)
	}
	response := httptest.NewRecorder()
	h.handler.ServeHTTP(response, req)
	if response.Code != status {
		h.t.Fatalf("%s %s returned %d instead of %d: %.600s", method, path, response.Code, status, response.Body.String())
	}
	return response
}

func (h *harness) activate(code string) *http.Cookie {
	h.t.Helper()
	out := h.call("POST", "/painel/ativar", nil, url.Values{"codigo": {code}, "senha": {fixturePassword}}, 303)
	cookie := out.Result().Cookies()[0]
	if !cookie.HttpOnly || cookie.SameSite != http.SameSiteLaxMode || cookie.Path != "/" {
		h.t.Fatal("unexpected session cookie policy")
	}
	return cookie
}

func (h *harness) primary() (*http.Cookie, string) {
	h.t.Helper()
	code, err := h.panel.Bootstrap(h.ctx, "primary@example.test", "Primary")
	require(h.t, err)
	cookie := h.activate(code)
	id := h.accountID("primary@example.test")
	return cookie, id
}

func (h *harness) accountID(email string) string {
	h.t.Helper()
	var id string
	require(h.t, h.pool.QueryRow(h.ctx, `SELECT id::text FROM users WHERE email=$1`, email).Scan(&id))
	return id
}

var activationLink = regexp.MustCompile(`/painel/#([A-Za-z0-9_-]{43})`)

func (h *harness) invite(owner *http.Cookie, role, email string) (*http.Cookie, string) {
	h.t.Helper()
	section := "equipe"
	if role == "user" {
		section = "usuarios"
	}
	out := h.call("POST", "/painel/"+section, owner, url.Values{
		"acao": {"convidar"}, "papel": {role}, "nome": {"Fixture " + role}, "email": {email},
	}, 200)
	match := activationLink.FindStringSubmatch(out.Body.String())
	if len(match) != 2 {
		h.t.Fatal("activation link absent from verified account creation")
	}
	cookie := h.activate(match[1])
	h.call("POST", "/painel/ativar", nil, url.Values{"codigo": {match[1]}, "senha": {fixturePassword}}, 401)
	return cookie, h.accountID(email)
}

func TestPanelIdentityIntegration(t *testing.T) {
	h := integration(t)
	h.call("GET", "/painel/", nil, nil, 200)
	primary, primaryID := h.primary()
	otherOwner, _ := h.invite(primary, "owner", "owner2@example.test")
	support, _ := h.invite(primary, "support", "support@example.test")
	user, userID := h.invite(primary, "user", "user@example.test")

	h.call("GET", "/painel/usuarios?formato=csv", user, nil, 403)
	h.call("GET", "/painel/usuarios?formato=csv", support, nil, 200)
	h.call("POST", "/painel/usuarios", support, url.Values{
		"motivo": {"access_review"}, "acao": {"atualizar"}, "estado": {"suspenso"}, "id": {userID},
	}, 403)
	h.call("POST", "/painel/equipe", otherOwner, url.Values{
		"motivo": {"access_review"}, "acao": {"atualizar"}, "papel": {"user"}, "estado": {"suspenso"}, "id": {primaryID},
	}, 409)
	h.call("POST", "/painel/usuarios", primary, url.Values{"acao": {"acesso"}, "id": {userID}}, 403)
	duplicate := h.call("POST", "/painel/usuarios", primary, url.Values{
		"acao": {"convidar"}, "nome": {"Nome mantido"}, "email": {"USER@example.test"},
	}, 409)
	if !strings.Contains(duplicate.Body.String(), "Nome mantido") {
		t.Fatal("failed form lost its values")
	}

	h.call("GET", "/painel/configuracoes", primary, nil, 200)
	h.call("POST", "/painel/configuracoes", primary, url.Values{
		"motivo": {"access_review"}, "papel": {"support"}, "permissao": {"reports.read"},
	}, 303)
	h.call("GET", "/painel/usuarios", support, nil, 403)
	h.call("GET", "/painel/disponibilizados", support, nil, 200)

	req := httptest.NewRequest("POST", testOrigin+"/painel/sair", nil)
	req.Header.Set("Origin", "https://outside.example.test")
	req.AddCookie(user)
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, req)
	if out.Code != 403 {
		t.Fatal(fmt.Sprintf("cross-origin form returned %d", out.Code))
	}
	h.call("POST", "/painel/sair", user, nil, 303)
	h.call("GET", "/painel/perfil", user, nil, 303)
	h.call("POST", "/painel/entrar", nil, url.Values{"email": {"user@example.test"}, "senha": {fixturePassword}}, 303)
}
