package httpapi

import (
	"context"
	"net/http"

	"vault/internal/service"
)

type verifier interface {
	Verify(ctx context.Context, code string, hmacKey string) (service.VerifyResult, error)
}

// VerifyHandler serves the public verification page data: GET /v/{code}.
// No auth. No photos. Only seal anchors the server can vouch for.
type VerifyHandler struct {
	svc     verifier
	hmacKey string
}

func NewVerifyHandler(svc verifier, hmacKey string) *VerifyHandler {
	return &VerifyHandler{svc: svc, hmacKey: hmacKey}
}

func (h *VerifyHandler) Verify(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.Verify(r.Context(), r.PathValue("code"), h.hmacKey)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
