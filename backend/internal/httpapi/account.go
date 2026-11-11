package httpapi

import (
	"context"
	"errors"
	"net/http"

	"vault/internal/credential"
	"vault/internal/httpapi/accountauth"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

type AccountHandler struct {
	Registration   *account.Registration
	Verification   *account.Verification
	Authentication *account.Authentication
	Credentials    *panel.Service
}

type accountActorKey struct{}

func (h *AccountHandler) Guard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		if h == nil || h.Authentication == nil {
			accountError(w, 503, "verification_unavailable")
			return
		}
		token, ok := accountauth.Bearer(r)
		if !ok {
			accountError(w, 401, "authentication_required")
			return
		}
		actor, err := h.Authentication.Session(r.Context(), token)
		if errors.Is(err, postgres.ErrNotFound) {
			accountError(w, 401, "authentication_required")
			return
		}
		if err != nil {
			accountError(w, 503, "verification_unavailable")
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), accountActorKey{}, actor)))
	})
}

func (h *AccountHandler) Profile(w http.ResponseWriter, r *http.Request) {
	a := r.Context().Value(accountActorKey{}).(postgres.PanelActor)
	writeJSON(w, 200, map[string]string{"id": a.ID, "email": a.Email, "name": a.Name})
}

func (h *AccountHandler) Logout(w http.ResponseWriter, r *http.Request) {
	token, _ := accountauth.Bearer(r)
	if err := h.Authentication.Logout(r.Context(), token); err != nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *AccountHandler) Register(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Registration == nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	var input struct {
		Email string `json:"email"`
	}
	if !accountInput(w, r, &input) {
		return
	}
	err := h.Registration.Register(r.Context(), input.Email, accountauth.Source(r))
	if errors.Is(err, account.ErrInput) {
		accountError(w, 400, "invalid_request")
		return
	}
	if err != nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	writeJSON(w, 202, map[string]string{"status": "verification_requested"})
}

func (h *AccountHandler) Verify(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Verification == nil || h.Credentials == nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	var input struct {
		Token    string `json:"token"`
		Password string `json:"password"`
	}
	if !accountInput(w, r, &input) || !h.limit(w, r, "") {
		return
	}
	err := h.Verification.Verify(r.Context(), input.Token, input.Password)
	if errors.Is(err, credential.ErrPolicy) {
		accountError(w, 400, "password_policy")
		return
	}
	if errors.Is(err, postgres.ErrNotFound) {
		accountError(w, 401, "invalid_verification")
		return
	}
	if err != nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	writeJSON(w, 200, map[string]string{"status": "email_verified"})
}

func (h *AccountHandler) Login(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Authentication == nil || h.Credentials == nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	var input struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if !accountInput(w, r, &input) || !h.limit(w, r, input.Email) {
		return
	}
	token, err := h.Authentication.Login(r.Context(), input.Email, input.Password)
	if errors.Is(err, postgres.ErrNotFound) {
		accountError(w, 401, "authentication_required")
		return
	}
	if err != nil {
		accountError(w, 503, "verification_unavailable")
		return
	}
	writeJSON(w, 200, map[string]any{"access_token": token, "token_type": "Bearer", "expires_in": account.AccessSeconds})
}
