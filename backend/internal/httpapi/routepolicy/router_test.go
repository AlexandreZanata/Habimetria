package routepolicy

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestPrivateRoutesRequireWorkingGuard(t *testing.T) {
	for _, class := range []Class{HostedOwner, AccountControl, StaffControl, ReportAccess} {
		for _, guard := range []Guard{nil, func(Rule, http.Handler) http.Handler { return nil }} {
			router := New([]Rule{{Pattern: "GET /private", Class: class}}, guard)
			router.HandleFunc("GET /private", func(http.ResponseWriter, *http.Request) {
				t.Fatal("private handler ran without authorization")
			})
			for _, method := range []string{"GET", "HEAD"} {
				out := httptest.NewRecorder()
				router.ServeHTTP(out, httptest.NewRequest(method, "/private", nil))
				if out.Code != http.StatusServiceUnavailable {
					t.Fatalf("%s %s without guard: %d", method, class, out.Code)
				}
			}
		}
	}
}

func TestPublicControlsAndRecipientsBypassPrivateGuard(t *testing.T) {
	for _, class := range []Class{PublicControl, PublicRecipient, Delegated} {
		router := New([]Rule{{Pattern: "GET /public", Class: class}}, func(Rule, http.Handler) http.Handler {
			t.Fatal("public control/recipient was gated by private authorization")
			return nil
		})
		router.HandleFunc("GET /public", func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(204) })
		out := httptest.NewRecorder()
		router.ServeHTTP(out, httptest.NewRequest("GET", "/public", nil))
		if out.Code != 204 {
			t.Fatalf("%s returned %d", class, out.Code)
		}
	}
}

func TestGuardReceivesPolicyAndCanDenyDispatch(t *testing.T) {
	want := Rule{Pattern: "POST /private/{id}", Class: HostedOwner, Scope: "reports"}
	var got Rule
	router := New([]Rule{want}, func(rule Rule, _ http.Handler) http.Handler {
		got = rule
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.PathValue("id") != "fixture" {
				t.Fatal("guard did not receive routed resource identity")
			}
			w.WriteHeader(http.StatusForbidden)
		})
	})
	router.HandleFunc(want.Pattern, func(http.ResponseWriter, *http.Request) { t.Fatal("denied handler ran") })
	inspected := router.Rules()
	inspected[0].Class = PublicControl
	inspected[0].Scope = "properties"
	out := httptest.NewRecorder()
	router.ServeHTTP(out, httptest.NewRequest("POST", "/private/fixture", nil))
	if out.Code != http.StatusForbidden || got != want || router.Rules()[0] != want {
		t.Fatal("inspection changed effective authorization")
	}
}

func TestInvalidOrUnclassifiedRegistrationFailsAtStartup(t *testing.T) {
	cases := map[string]func(){
		"empty pattern":   func() { New([]Rule{{Class: PublicControl}}, nil) },
		"unknown class":   func() { New([]Rule{{Pattern: "GET /unknown", Class: "other"}}, nil) },
		"scope on public": func() { New([]Rule{{Pattern: "GET /public", Class: PublicControl, Scope: "reports"}}, nil) },
		"duplicate policy": func() {
			rule := Rule{Pattern: "GET /public", Class: PublicControl}
			New([]Rule{rule, rule}, nil)
		},
		"unclassified": func() { New(nil, nil).HandleFunc("GET /unknown", func(http.ResponseWriter, *http.Request) {}) },
		"nil handler": func() {
			New([]Rule{{Pattern: "GET /public", Class: PublicControl}}, nil).Handle("GET /public", nil)
		},
		"nil function": func() {
			New([]Rule{{Pattern: "GET /public", Class: PublicControl}}, nil).HandleFunc("GET /public", nil)
		},
	}
	for name, run := range cases {
		t.Run(name, func(t *testing.T) {
			defer func() {
				if recover() == nil {
					t.Fatal("invalid registration was accepted")
				}
			}()
			run()
		})
	}
}
