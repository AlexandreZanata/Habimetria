package panelweb

import (
	"net/http/httptest"
	"testing"

	"vault/internal/httpapi/routepolicy"
)

func TestPanelUsesConcreteClassifiedRoutes(t *testing.T) {
	p, err := New(nil, nil, "http://localhost:8080")
	if err != nil {
		t.Fatal(err)
	}
	want := map[string]bool{
		"GET /painel/{$}": true, "GET /painel/assets/": true,
		"POST /painel/entrar": true, "POST /painel/ativar": true, "POST /painel/sair": true,
	}
	counts := map[routepolicy.Class]int{}
	router := p.classifiedRoutes()
	for _, rule := range router.Rules() {
		counts[rule.Class]++
		if rule.Class == routepolicy.PublicControl {
			if !want[rule.Pattern] {
				t.Fatalf("unexpected public panel route: %s", rule.Pattern)
			}
			delete(want, rule.Pattern)
		}
		if rule.Pattern == "GET /painel/{section}" || rule.Pattern == "POST /painel/{section}" {
			t.Fatal("generic section route bypasses concrete policy review")
		}
	}
	if len(want) != 0 || len(router.Rules()) != 23 {
		t.Fatalf("incomplete panel allowlist: missing=%v routes=%d", want, len(router.Rules()))
	}
	for class, want := range map[routepolicy.Class]int{routepolicy.AccountControl: 2, routepolicy.HostedOwner: 6, routepolicy.StaffControl: 9, routepolicy.ReportAccess: 1} {
		if counts[class] != want {
			t.Fatalf("%s has %d routes instead of %d", class, counts[class], want)
		}
	}
	// Unknown sections/methods must not reach account lookup or any service.
	for _, request := range []struct {
		method, path string
		status       int
	}{
		{"GET", "/painel/unknown", 404}, {"POST", "/painel/minha-area", 405},
	} {
		out := httptest.NewRecorder()
		router.ServeHTTP(out, httptest.NewRequest(request.method, request.path, nil))
		if out.Code != request.status {
			t.Fatalf("%s %s returned %d", request.method, request.path, out.Code)
		}
	}
}
