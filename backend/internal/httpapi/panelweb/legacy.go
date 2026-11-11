package panelweb

import (
	"errors"
	"net/http"

	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

// LegacyPolicy defaults to owner-only access independently of panel exposure.
// The development exception must come from validated application configuration.
type LegacyPolicy struct{ AllowUnclaimedDevelopment bool }

func ProtectLegacy(s *panel.Service, origin string, policy LegacyPolicy) func(string, http.HandlerFunc) http.HandlerFunc {
	return func(scope string, next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Cache-Control", "no-store")
			if s == nil {
				http.Error(w, "authorization unavailable", http.StatusServiceUnavailable)
				return
			}
			allowUnclaimed := policy.AllowUnclaimedDevelopment && origin == ""
			if r.PathValue("id") == "" {
				if scope != "properties" || !allowUnclaimed {
					http.Error(w, "create the property through the authenticated panel", http.StatusForbidden)
					return
				}
				next(w, r)
				return
			}
			owner, err := s.ResourceOwner(r.Context(), scope, r.PathValue("id"))
			if errors.Is(err, panel.ErrInput) {
				http.Error(w, "invalid resource", http.StatusBadRequest)
				return
			}
			if errors.Is(err, postgres.ErrNotFound) {
				http.NotFound(w, r)
				return
			}
			if err != nil {
				http.Error(w, "resource unavailable", http.StatusInternalServerError)
				return
			}
			if owner == nil && !allowUnclaimed {
				http.NotFound(w, r)
				return
			}
			if owner != nil {
				actor, err := s.Session(r.Context(), sessionValue(r, cookieName(origin)))
				if errors.Is(err, postgres.ErrNotFound) || (err == nil && actor.ID != *owner) {
					http.NotFound(w, r)
					return
				}
				if err != nil {
					http.Error(w, "authorization unavailable", http.StatusInternalServerError)
					return
				}
				if r.Method != "GET" && r.Method != "HEAD" && (origin == "" || r.Header.Get("Origin") != origin) {
					http.Error(w, "invalid origin", http.StatusForbidden)
					return
				}
			}
			next(w, r)
		}
	}
}
