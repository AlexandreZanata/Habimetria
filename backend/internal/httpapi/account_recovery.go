package httpapi

import (
	"errors"
	"net/http"

	"vault/internal/credential"
	"vault/internal/httpapi/accountauth"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

func (h *AccountHandler) Recover(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Recovery == nil {
		accountError(w, 503, "recovery_unavailable")
		return
	}
	var input struct {
		Email string `json:"email"`
	}
	if !accountInput(w, r, &input) {
		return
	}
	err := h.Recovery.Request(r.Context(), input.Email, accountauth.Source(r))
	if errors.Is(err, account.ErrInput) {
		accountError(w, 400, "invalid_request")
		return
	}
	if err != nil {
		accountError(w, 503, "recovery_unavailable")
		return
	}
	writeJSON(w, 202, map[string]string{"status": "recovery_requested"})
}

func (h *AccountHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.PasswordReset == nil || h.Credentials == nil {
		accountError(w, 503, "recovery_unavailable")
		return
	}
	var input struct {
		Token        string `json:"token"`
		Password     string `json:"password"`
		Confirmation string `json:"password_confirmation"`
	}
	if !accountInput(w, r, &input) || !h.limit(w, r, "") {
		return
	}
	err := h.PasswordReset.Reset(r.Context(), input.Token, input.Password, input.Confirmation)
	switch {
	case errors.Is(err, credential.ErrPolicy):
		accountError(w, 400, "password_policy")
	case errors.Is(err, account.ErrConfirmation):
		accountError(w, 400, "password_confirmation")
	case errors.Is(err, postgres.ErrNotFound):
		accountError(w, 401, "invalid_recovery")
	case err != nil:
		accountError(w, 503, "recovery_unavailable")
	default:
		writeJSON(w, 200, map[string]string{"status": "password_updated"})
	}
}
