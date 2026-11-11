package panelweb_test

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/httpapi"
	"vault/internal/httpapi/panelweb"
	"vault/internal/httpapi/routepolicy"
)

// Use real owner/session queries with a dispatch sentinel: rejection must
// happen before any private handler reads/writes data or issues object URLs.
func TestLegacyOwnerPolicyIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	for _, unclaimed := range []bool{false, true} {
		if unclaimed {
			_, err := f.h.pool.Exec(f.h.ctx, `UPDATE properties SET owner_user_id=NULL WHERE id=$1`, f.ids["properties"])
			require(t, err)
		}
		for _, origin := range []string{testOrigin, ""} {
			for _, rule := range httpapi.NewRouter(httpapi.Deps{}).Rules() {
				if rule.Class != routepolicy.HostedOwner {
					continue
				}
				method, path, _ := strings.Cut(rule.Pattern, " ")
				path = strings.ReplaceAll(path, "{id}", f.ids[rule.Scope])
				guard := panelweb.ProtectLegacy(f.h.panel, origin, panelweb.LegacyPolicy{})
				mux := http.NewServeMux()
				mux.HandleFunc(rule.Pattern, guard(rule.Scope, func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204) }))
				for _, cookie := range []*http.Cookie{nil, f.other, f.staff, f.owner} {
					want := http.StatusNotFound
					if path == "/api/v1/properties" {
						want = http.StatusForbidden
					} else if !unclaimed && cookie == f.owner {
						want = http.StatusNoContent
						if method != "GET" && origin == "" {
							want = http.StatusForbidden
						}
					}
					methods := []string{method}
					if method == "GET" {
						methods = append(methods, "HEAD")
					}
					for _, verb := range methods {
						req := httptest.NewRequest(verb, testOrigin+path+"?owner_user_id="+f.ownerID, strings.NewReader(`{"owner_user_id":"`+f.ownerID+`"}`)).WithContext(f.h.ctx)
						req.Header.Set("Origin", testOrigin)
						req.Header.Set("X-User-ID", f.ownerID)
						req.Header.Set("X-Worker-Subject", f.ownerID)
						if cookie != nil {
							req.AddCookie(cookie)
						}
						out := httptest.NewRecorder()
						mux.ServeHTTP(out, req)
						if out.Code != want || out.Header().Get("Cache-Control") != "no-store" {
							t.Fatalf("%s unclaimed=%v panel=%q owner=%v: %d want %d", rule.Pattern, unclaimed, origin, cookie == f.owner, out.Code, want)
						}
					}
				}
			}
		}
	}
}

func TestLegacyGuardFailuresIntegration(t *testing.T) {
	h := integration(t)
	for _, tc := range []struct {
		scope, id string
		want      int
	}{
		{"inspections", "not-an-id", 400},
		{"unknown-scope", "00000000-0000-0000-0000-000000000001", 500},
		{"rooms", "00000000-0000-0000-0000-000000000001", 404},
		{"inspections", "", 403},
	} {
		req := httptest.NewRequest("GET", testOrigin, nil).WithContext(h.ctx)
		req.SetPathValue("id", tc.id)
		out := httptest.NewRecorder()
		panelweb.ProtectLegacy(h.panel, "", panelweb.LegacyPolicy{})(tc.scope,
			func(http.ResponseWriter, *http.Request) { t.Fatal("authorization failure dispatched") })(out, req)
		if out.Code != tc.want {
			t.Fatalf("scope=%s id=%s: %d want %d", tc.scope, tc.id, out.Code, tc.want)
		}
	}
	h.pool.Close()
	req := httptest.NewRequest("GET", testOrigin, nil).WithContext(h.ctx)
	req.SetPathValue("id", "00000000-0000-0000-0000-000000000001")
	out := httptest.NewRecorder()
	panelweb.ProtectLegacy(h.panel, "", panelweb.LegacyPolicy{})("rooms",
		func(http.ResponseWriter, *http.Request) { t.Fatal("DB failure dispatched") })(out, req)
	if out.Code != 500 {
		t.Fatalf("DB failure: %d", out.Code)
	}
}

func TestLegacySessionFailureIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	_, err := f.h.pool.Exec(f.h.ctx, `ALTER TABLE panel_sessions RENAME TO unavailable_sessions`)
	require(t, err)
	req := httptest.NewRequest("GET", testOrigin, nil).WithContext(f.h.ctx)
	req.SetPathValue("id", f.ids["properties"])
	req.AddCookie(f.owner)
	out := httptest.NewRecorder()
	panelweb.ProtectLegacy(f.h.panel, testOrigin, panelweb.LegacyPolicy{})("properties",
		func(http.ResponseWriter, *http.Request) { t.Fatal("session DB failure dispatched") })(out, req)
	if out.Code != 500 || strings.Contains(out.Body.String(), "panel_sessions") {
		t.Fatal("session failure must fail closed without SQL details")
	}
}
