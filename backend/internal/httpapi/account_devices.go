package httpapi

import (
	"errors"
	"net/http"

	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

func deviceError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, postgres.ErrNotFound):
		accountError(w, 401, "authentication_required")
	case errors.Is(err, account.ErrInput):
		accountError(w, 400, "invalid_request")
	case errors.Is(err, postgres.ErrSessionLimit):
		accountError(w, 409, "session_limit")
	default:
		accountError(w, 503, "verification_unavailable")
	}
}
func (h *AccountHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Authentication == nil || h.Credentials == nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	var input struct {
		Refresh string `json:"refresh_token"`
		Device  string `json:"device_id"`
	}
	if !accountInput(w, r, &input) || !h.limit(w, r, "") {
		return
	}
	pair, err := h.Authentication.Refresh(r.Context(), input.Refresh, input.Device)
	if err != nil {
		deviceError(w, err)
		return
	}
	writeJSON(w, 200, pair)
}
func (h *AccountHandler) Devices(w http.ResponseWriter, r *http.Request) {
	a := r.Context().Value(accountActorKey{}).(postgres.PanelActor)
	devices, err := h.Authentication.Devices(r.Context(), a.ID)
	if err != nil {
		deviceError(w, err)
		return
	}
	writeJSON(w, 200, map[string]any{"sessions": devices})
}
func (h *AccountHandler) RevokeDevice(w http.ResponseWriter, r *http.Request) {
	a := r.Context().Value(accountActorKey{}).(postgres.PanelActor)
	err := h.Authentication.RevokeDevice(r.Context(), a.ID, r.PathValue("id"))
	if errors.Is(err, postgres.ErrNotFound) {
		accountError(w, 404, "not_found")
		return
	}
	if err != nil {
		deviceError(w, err)
		return
	}
	w.WriteHeader(204)
}
