package httpapi

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/httpapi/routepolicy"
)

func TestAPIPublicRouteAllowlist(t *testing.T) {
	want := map[string]routepolicy.Class{
		"POST /api/v1/account/sessions/refresh":   routepolicy.PublicControl,
		"GET /api/v1/account/sessions":            routepolicy.AccountControl,
		"DELETE /api/v1/account/sessions/{id}":    routepolicy.AccountControl,
		"POST /api/v1/account/recovery":           routepolicy.PublicControl,
		"POST /api/v1/account/password":           routepolicy.PublicControl,
		"POST /api/v1/account/registrations":      routepolicy.PublicControl,
		"POST /api/v1/account/verification":       routepolicy.PublicControl,
		"POST /api/v1/account/sessions":           routepolicy.PublicControl,
		"GET /api/v1/account/profile":             routepolicy.AccountControl,
		"DELETE /api/v1/account/sessions/current": routepolicy.AccountControl,
		"GET /healthz":                            routepolicy.PublicControl, "GET /readyz": routepolicy.PublicControl,
		"GET /api/v1/ping": routepolicy.PublicControl, "/painel/": routepolicy.Delegated,
		"GET /v/{code}": routepolicy.PublicRecipient, "GET /s/{token}": routepolicy.PublicRecipient,
		"GET /s/{token}/pdf": routepolicy.PublicRecipient,
	}
	router := NewRouter(Deps{Panel: http.NotFoundHandler()})
	private := 0
	for _, rule := range router.Rules() {
		if rule.Class == routepolicy.HostedOwner {
			private++
			if rule.Scope == "" {
				t.Fatalf("owner route has no owner lookup: %s", rule.Pattern)
			}
			continue
		}
		if want[rule.Pattern] != rule.Class {
			t.Fatalf("unexpected public/delegated classification: %+v", rule)
		}
		delete(want, rule.Pattern)
	}
	if len(want) != 0 || private != 24 {
		t.Fatalf("API inventory changed without reviewed coverage: missing=%v private=%d", want, private)
	}
}

func TestEveryPrivateAPIRequiresConfiguredAuthorization(t *testing.T) {
	for _, deps := range []Deps{{}, {Guard: func(string, http.HandlerFunc) http.HandlerFunc { return nil }}} {
		router := NewRouter(deps)
		for _, rule := range router.Rules() {
			if rule.Class != routepolicy.HostedOwner {
				continue
			}
			method, path, _ := strings.Cut(rule.Pattern, " ")
			path = strings.ReplaceAll(path, "{id}", "fixture")
			out := httptest.NewRecorder()
			router.ServeHTTP(out, httptest.NewRequest(method, path, nil))
			if out.Code != http.StatusServiceUnavailable {
				t.Fatalf("unguarded %s returned %d", rule.Pattern, out.Code)
			}
		}
	}
}

func TestEveryOwnerRouteDispatchesThroughItsScope(t *testing.T) {
	router := NewRouter(Deps{Guard: func(scope string, _ http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, _ *http.Request) {
			w.Header().Set("X-Test-Scope", scope)
			w.WriteHeader(http.StatusForbidden)
		}
	}})
	counts := map[string]int{}
	for _, rule := range router.Rules() {
		if rule.Class != routepolicy.HostedOwner {
			continue
		}
		method, path, _ := strings.Cut(rule.Pattern, " ")
		out := httptest.NewRecorder()
		router.ServeHTTP(out, httptest.NewRequest(method, strings.ReplaceAll(path, "{id}", "fixture"), nil))
		if out.Code != 403 || out.Header().Get("X-Test-Scope") != rule.Scope {
			t.Fatalf("owner guard bypassed on %s", rule.Pattern)
		}
		counts[rule.Scope]++
	}
	for scope, want := range map[string]int{"properties": 4, "tenancies": 3, "inspections": 8, "rooms": 2, "reports": 5, "occurrences": 2} {
		if counts[scope] != want {
			t.Fatalf("%s scope has %d routes instead of %d", scope, counts[scope], want)
		}
	}
}
