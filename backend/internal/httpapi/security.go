package httpapi

import "net/http"

// SecurityHeaders sets baseline response headers on every route:
// no MIME sniffing, no framing, no referrer leakage. HSTS stays out:
// it is only valid behind TLS, which Compose does not terminate here.
func SecurityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		next.ServeHTTP(w, r)
	})
}
