package accountauth

import (
	"net/http/httptest"
	"strings"
	"testing"
)

func TestNativeTransport(t *testing.T) {
	for _, headers := range []map[string][]string{
		{"Authorization": {"Bearer " + strings.Repeat("a", 43)}},
		{"Authorization": {"Bearer " + strings.Repeat("a", 43)}, "Cookie": {""}},
		{"Authorization": {"Bearer " + strings.Repeat("a", 43)}, "Origin": {""}},
		{"Authorization": {"Bearer " + strings.Repeat("a", 43), "Bearer " + strings.Repeat("b", 43)}},
		{"Authorization": {"Basic " + strings.Repeat("a", 43)}},
		{"Authorization": {"Bearer short"}},
		{"Authorization": {"Bearer " + strings.Repeat("a", 42) + " "}},
	} {
		r := httptest.NewRequest("GET", "http://localhost/", nil)
		for key, values := range headers {
			r.Header[key] = values
		}
		_, valid := Bearer(r)
		want := len(headers) == 1 && len(headers["Authorization"]) == 1 && headers["Authorization"][0] == "Bearer "+strings.Repeat("a", 43)
		if valid != want {
			t.Fatal("mixed or invalid transport accepted")
		}
	}
	for _, peer := range []struct{ remote, want string }{
		{"[::ffff:192.0.2.2]:1234", "192.0.2.2"}, {"[2001:db8::1]:42", "2001:db8::1"}, {"192.0.2.1", "192.0.2.1"}, {"invalid", ""},
	} {
		r := httptest.NewRequest("GET", "http://localhost/", nil)
		r.RemoteAddr = peer.remote
		r.Header.Set("X-Forwarded-For", "203.0.113.1")
		if Source(r) != peer.want {
			t.Fatal("connection authority changed")
		}
	}
}
