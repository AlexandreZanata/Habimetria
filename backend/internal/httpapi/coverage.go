package httpapi

import (
	"context"
	"net/http"

	"vault/internal/service"
)

type coverageGetter interface {
	Get(ctx context.Context, inspectionID string) (service.Coverage, error)
}

// CoverageHandler serves GET /api/v1/inspections/{id}/coverage.
type CoverageHandler struct {
	svc coverageGetter
}

func NewCoverageHandler(svc coverageGetter) *CoverageHandler {
	return &CoverageHandler{svc: svc}
}

func (h *CoverageHandler) Get(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.Get(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
