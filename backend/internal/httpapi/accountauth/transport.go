// Package accountauth validates native authentication inputs at HTTP boundaries.
package accountauth

import (
	"net"
	"net/http"
	"net/netip"
	"strings"
)

// Source trusts only the connection peer, never client-controlled proxy headers.
func Source(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		host = r.RemoteAddr
	}
	ip, err := netip.ParseAddr(host)
	if err != nil {
		return ""
	}
	return ip.Unmap().String()
}

func Native(r *http.Request) bool {
	return len(r.Header.Values("Origin")) == 0 && len(r.Header.Values("Cookie")) == 0
}

func Bearer(r *http.Request) (string, bool) {
	values := r.Header.Values("Authorization")
	if !Native(r) || len(values) != 1 {
		return "", false
	}
	scheme, token, ok := strings.Cut(values[0], " ")
	return token, ok && strings.EqualFold(scheme, "Bearer") && len(token) == 43 && !strings.ContainsAny(token, " \t\r\n")
}
