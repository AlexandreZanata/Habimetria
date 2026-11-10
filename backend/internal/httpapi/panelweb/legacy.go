package panelweb

import (
	"errors"
	"net/http"

	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

// ProtectLegacy requires the Property owner's session for claimed data.
// Unclaimed demo resources are available only with the panel disabled.
func ProtectLegacy(s *panel.Service, origin string) func(string, http.HandlerFunc) http.HandlerFunc {
	return func(scope string, next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			if r.PathValue("id") == "" {
				if origin != "" {
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
			if owner == nil && origin != "" {
				http.NotFound(w, r)
				return
			}
			if owner != nil {
				w.Header().Set("Cache-Control", "no-store")
				actor, err := s.Session(r.Context(), sessionValue(r, cookieName(origin)))
				if err != nil || actor.ID != *owner {
					http.NotFound(w, r)
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
