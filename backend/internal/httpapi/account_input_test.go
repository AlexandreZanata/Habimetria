package httpapi

import (
	"net/http/httptest"
	"strings"
	"testing"
)

func TestAccountJSONBounds(t *testing.T) {
	for _, tc := range []struct {
		body, media, header, value string
		status                     int
	}{
		{`{"email":"user@example.test"}`, "application/json", "", "", 200},
		{`null`, "application/json", "", "", 400},
		{`{}`, "text/plain", "", "", 415},
		{`{} {}`, "application/json", "", "", 400},
		{`{"owner_user_id":"forged"}`, "application/json", "", "", 400},
		{`{"email":"` + strings.Repeat("a", 4096) + `"}`, "application/json", "", "", 413},
		{`{}`, "application/json", "Cookie", "session=ambient", 403},
		{`{}`, "application/json", "Origin", "http://localhost", 403},
		{`{}`, "application/json", "Authorization", "Bearer forged", 403},
	} {
		request := httptest.NewRequest("POST", "http://localhost/", strings.NewReader(tc.body))
		request.Header.Set("Content-Type", tc.media)
		if tc.header != "" {
			request.Header.Set(tc.header, tc.value)
		}
		out := httptest.NewRecorder()
		var input struct {
			Email string `json:"email"`
		}
		ok := accountInput(out, request, &input)
		if ok {
			out.WriteHeader(200)
		}
		if out.Code != tc.status || ok != (tc.status == 200) || out.Header().Get("Cache-Control") != "no-store" {
			t.Fatalf("bound failed: status %d want %d", out.Code, tc.status)
		}
	}
}

func TestAbsentAccountServices(t *testing.T) {
	router := NewRouter(Deps{})
	for _, tc := range []struct{ method, path string }{
		{"POST", "registrations"}, {"POST", "verification"}, {"POST", "sessions"}, {"GET", "profile"}, {"DELETE", "sessions/current"},
		{"POST", "recovery"}, {"POST", "password"},
	} {
		out := httptest.NewRecorder()
		router.ServeHTTP(out, httptest.NewRequest(tc.method, "http://localhost/api/v1/account/"+tc.path, nil))
		if out.Code != 503 {
			t.Fatal("missing account authority was accepted", out.Code)
		}
	}
}
