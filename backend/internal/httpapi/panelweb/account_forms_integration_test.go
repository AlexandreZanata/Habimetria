package panelweb_test

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"vault/internal/httpapi/panelweb"
)

func TestAccountFormsIntegration(t *testing.T) {
	h := integration(t)
	owner, _ := h.primary()
	h.call("GET", "/painel/cadastro", nil, nil, 200)
	newAccount := h.call("POST", "/painel/cadastro", nil, url.Values{"email": {"customer@example.test"}}, 200)
	existing := h.call("POST", "/painel/cadastro", nil, url.Values{"email": {"primary@example.test"}}, 200)
	if newAccount.Body.String() != existing.Body.String() {
		t.Fatal("registration enumerated identity")
	}
	h.call("POST", "/painel/cadastro", nil, url.Values{"email": {"invalid"}}, 400)
	proof := h.proof("customer@example.test")
	get := h.call("GET", "/painel/verificar", owner, nil, 200)
	if strings.Contains(get.Body.String(), proof) {
		t.Fatal("GET reflected proof")
	}
	h.call("GET", "/painel/entrar", owner, nil, 200)
	weak := h.call("POST", "/painel/verificar", owner, url.Values{"codigo": {proof}, "senha": {"short"}}, 400)
	if !strings.Contains(weak.Body.String(), "value=\""+proof+"\"") {
		t.Fatal("policy failure lost proof for retry")
	}
	r := httptest.NewRequest("POST", testOrigin+"/painel/verificar", strings.NewReader(url.Values{"codigo": {proof}, "senha": {fixturePassword}}.Encode()))
	r.Header.Set("Origin", "https://untrusted.example.test")
	r.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, r)
	if out.Code != 403 {
		t.Fatal("cross-origin verification accepted")
	}
	verified := h.call("POST", "/painel/verificar", owner, url.Values{"codigo": {proof}, "senha": {fixturePassword}}, 200)
	if len(verified.Result().Cookies()) != 0 || !strings.Contains(verified.Body.String(), "Sua conta está pronta") {
		t.Fatal("verification changed browser session or missed result")
	}
	h.call("POST", "/painel/verificar", nil, url.Values{"codigo": {proof}, "senha": {fixturePassword}}, 401)
	login := h.call("POST", "/painel/entrar", nil, url.Values{"email": {"customer@example.test"}, "senha": {fixturePassword}}, 303)
	cookie := login.Result().Cookies()[0]
	h.call("GET", "/painel/perfil", cookie, nil, 200)
	h.call("GET", "/painel/usuarios", cookie, nil, 403)
	h.call("GET", "/painel/usuarios", owner, nil, 200)
	// Mail-disabled rollback preserves login and already-issued verification.
	web, err := panelweb.NewConnected(h.panel, nil, testOrigin, panelweb.AccountServices{Verification: accountServices(t, h.pool, h.panel).Verification})
	require(t, err)
	request := httptest.NewRequest("GET", testOrigin+"/painel/cadastro", nil)
	out = httptest.NewRecorder()
	web.Routes().ServeHTTP(out, request)
	if out.Code != 503 {
		t.Fatal("disabled signup rendered a dead form")
	}
	request = httptest.NewRequest("GET", testOrigin+"/painel/entrar", nil)
	out = httptest.NewRecorder()
	web.Routes().ServeHTTP(out, request)
	if out.Code != http.StatusOK || strings.Contains(out.Body.String(), "Ainda não tem conta?") {
		t.Fatal("disabled signup exposed a control")
	}
}
