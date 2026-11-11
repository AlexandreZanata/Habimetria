package panelweb_test

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

func TestNativeOwnerBoundaryIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	h := f.h
	owner := h.access("boundary-alice@example.test")
	other := h.access("boundary-bob@example.test")
	path := "/api/v1/reports/" + f.ids["reports"]
	h.json("GET", path, "", owner, 200)
	h.json("GET", path, "", other, 404)
	h.json("GET", path, "", owner, 404, http.Header{"Cookie": {f.owner.String()}})
	h.json("GET", path, "", f.owner.Value, 404)
	h.json("GET", path, "", owner, 404, http.Header{"Origin": {testOrigin}})
	h.json("GET", path, "", "", 404, http.Header{"Cookie": {"habimetria_local=" + owner}})
	h.json("POST", "/api/v1/inspections/"+f.ids["inspections"]+"/rooms", `{"key":"new","label":"New room"}`, owner, 409)
	r := httptest.NewRequest("GET", testOrigin+"/painel/perfil", nil)
	r.Header.Set("Authorization", "Bearer "+owner)
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, r)
	if out.Code != 303 {
		t.Fatal("bearer authorized browser page")
	}
	h.json("DELETE", "/api/v1/account/sessions/current", "", owner, 204)
	h.json("GET", path, "", owner, 404)
	h.json("GET", "/api/v1/account/profile", "", other, 200)
}

func TestSharedAuthenticationThrottleIntegration(t *testing.T) {
	h := integration(t)
	primary, _ := h.primary()
	h.invite(primary, "user", "limited@example.test")
	body := `{"email":"limited@example.test","password":"wrong fixture password"}`
	for range 5 {
		h.json("POST", "/api/v1/account/sessions", body, "", 401)
	}
	for range 5 {
		h.call("POST", "/painel/entrar", nil, url.Values{"email": {"LIMITED@example.test"}, "senha": {"wrong fixture password"}}, 401)
	}
	out := h.json("POST", "/api/v1/account/sessions", body, "", 429)
	if out.Header().Get("Retry-After") != "900" {
		t.Fatal("missing backoff")
	}
	h.call("POST", "/painel/entrar", nil, url.Values{"email": {"limited@example.test"}, "senha": {fixturePassword}}, 429)
}
