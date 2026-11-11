package panelweb_test

import (
	"net/http"
	"net/url"
	"testing"

	"vault/internal/httpapi/panelweb"
)

func TestLegacyOwnerSessionRevocationIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	guard := panelweb.ProtectLegacy(f.h.panel, testOrigin, panelweb.LegacyPolicy{})
	check := func(cookie *http.Cookie, want int) {
		t.Helper()
		for _, verb := range []string{"GET", "HEAD", "POST"} {
			out := legacyGuardResult(guard, "reports", newLegacyRequest(f.h, verb, f.ids["reports"], cookie), t)
			if out.Code != want {
				t.Fatalf("%s retained revoked owner authority: %d want %d", verb, out.Code, want)
			}
		}
	}
	check(f.owner, 204)
	f.h.call("POST", "/painel/usuarios", f.staff, url.Values{"acao": {"atualizar"}, "id": {f.ownerID}, "estado": {"suspenso"}}, 303)
	check(f.owner, 404)
	f.h.call("POST", "/painel/usuarios", f.staff, url.Values{"acao": {"atualizar"}, "id": {f.ownerID}, "estado": {"ativo"}}, 303)
	check(f.owner, 404) // Reactivation cannot resurrect an old session.
	login := f.h.call("POST", "/painel/entrar", nil, url.Values{"email": {"boundary-alice@example.test"}, "senha": {fixturePassword}}, 303)
	fresh := login.Result().Cookies()[0]
	check(fresh, 204)
	f.h.call("POST", "/painel/sair", fresh, nil, 303)
	check(fresh, 404)
}

func TestReportCapabilityAndRoleRevocationIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	support, supportID := f.h.invite(f.staff, "support", "legacy-support@example.test")
	path := "/painel/documentos/" + f.ids["reports"]
	f.h.call("POST", "/painel/relatorios", f.owner, url.Values{"acao": {"disponibilizar"}, "id": {f.ids["reports"]}}, 303)
	f.h.call("GET", path, support, nil, 200)
	f.h.call("POST", "/painel/configuracoes", f.staff, url.Values{"papel": {"support"}, "permissao": {"accounts.read"}}, 303)
	f.h.call("GET", path, support, nil, 404)
	f.h.call("GET", path+"?baixar=1", support, nil, 404)
	f.h.call("POST", "/painel/configuracoes", f.staff, url.Values{"papel": {"support"}, "permissao": {"reports.read"}}, 303)
	f.h.call("GET", path, support, nil, 200)
	// Even an explicit Report grant never authorizes the private resource API.
	if out := f.h.boundaryRequest("GET", "/api/v1/reports/"+f.ids["reports"], support, ""); out.Code != 404 {
		t.Fatal("staff grant escaped Report-specific panel access")
	}
	f.h.call("POST", "/painel/equipe", f.staff, url.Values{"acao": {"atualizar"}, "id": {supportID}, "papel": {"user"}, "estado": {"ativo"}}, 303)
	f.h.call("GET", path, support, nil, 404)
	f.h.call("GET", path, f.owner, nil, 200)
}
