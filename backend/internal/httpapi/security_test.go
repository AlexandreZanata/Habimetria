package httpapi

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestSecurityHeadersPresent(t *testing.T) {
	inner := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusTeapot)
	})
	rec := httptest.NewRecorder()
	SecurityHeaders(inner).ServeHTTP(rec, httptest.NewRequest("GET", "/", nil))
	if rec.Code != http.StatusTeapot {
		t.Fatalf("inner handler must run, got %d", rec.Code)
	}
	for header, want := range map[string]string{
		"X-Content-Type-Options": "nosniff",
		"X-Frame-Options":        "DENY",
		"Referrer-Policy":        "no-referrer",
	} {
		if got := rec.Header().Get(header); got != want {
			t.Fatalf("%s: want %q, got %q", header, want, got)
		}
	}
}

func TestRouterWiresPrivateResourcesAndPanel(t *testing.T) {
	router := NewRouter(Deps{
		Guard: func(scope string, _ http.HandlerFunc) http.HandlerFunc {
			return func(w http.ResponseWriter, _ *http.Request) {
				w.Header().Set("X-Test-Scope", scope)
				w.WriteHeader(http.StatusForbidden)
			}
		},
		Panel: http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
			w.WriteHeader(http.StatusNoContent)
		}),
	})
	for _, route := range []struct{ method, path, scope string }{
		{"POST", "/api/v1/properties", "properties"},
		{"POST", "/api/v1/tenancies/id/inspections", "tenancies"},
		{"GET", "/api/v1/inspections/id/evidences/download-url", "inspections"},
		{"POST", "/api/v1/rooms/id/findings", "rooms"},
		{"GET", "/api/v1/reports/id/download-url", "reports"},
		{"GET", "/api/v1/occurrences/id/evidences", "occurrences"},
	} {
		res := httptest.NewRecorder()
		router.ServeHTTP(res, httptest.NewRequest(route.method, route.path, nil))
		if res.Code != http.StatusForbidden || res.Header().Get("X-Test-Scope") != route.scope {
			t.Errorf("%s %s is not protected by the %s scope", route.method, route.path, route.scope)
		}
	}
	for path, want := range map[string]int{"/painel/": http.StatusNoContent, "/api/v1/ping": http.StatusOK} {
		res := httptest.NewRecorder()
		router.ServeHTTP(res, httptest.NewRequest(http.MethodGet, path, nil))
		if res.Code != want || res.Header().Get("X-Test-Scope") != "" {
			t.Errorf("%s returned %d with scope %q", path, res.Code, res.Header().Get("X-Test-Scope"))
		}
	}
}
