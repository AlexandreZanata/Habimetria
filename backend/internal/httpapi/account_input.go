package httpapi

import (
	"encoding/json"
	"errors"
	"io"
	"mime"
	"net/http"

	"vault/internal/httpapi/accountauth"
	"vault/internal/store/postgres"
)

func accountError(w http.ResponseWriter, status int, code string) {
	w.Header().Set("Cache-Control", "no-store")
	if status == 401 {
		w.Header().Set("WWW-Authenticate", "Bearer")
	}
	writeJSON(w, status, map[string]string{"error": code, "code": code})
}

func accountInput[T any](w http.ResponseWriter, r *http.Request, input *T) bool {
	w.Header().Set("Cache-Control", "no-store")
	if !accountauth.Native(r) || len(r.Header.Values("Authorization")) != 0 {
		accountError(w, 403, "invalid_transport")
		return false
	}
	media, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
	if err != nil || media != "application/json" {
		accountError(w, 415, "invalid_content_type")
		return false
	}
	r.Body = http.MaxBytesReader(w, r.Body, 4096)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	var decoded *T
	err = decoder.Decode(&decoded)
	if err == nil && decoded != nil {
		var extra any
		err = decoder.Decode(&extra)
		if err == io.EOF {
			*input = *decoded
			return true
		}
	}
	var tooLarge *http.MaxBytesError
	if errors.As(err, &tooLarge) {
		accountError(w, 413, "request_too_large")
	} else {
		accountError(w, 400, "invalid_request")
	}
	return false
}

func (h *AccountHandler) limit(w http.ResponseWriter, r *http.Request, email string) bool {
	err := h.Credentials.AllowAuthentication(r.Context(), accountauth.Source(r), email)
	if errors.Is(err, postgres.ErrPanelRate) {
		w.Header().Set("Retry-After", "900")
		accountError(w, 429, "too_many_attempts")
		return false
	}
	if err != nil {
		accountError(w, 503, "verification_unavailable")
		return false
	}
	return true
}
