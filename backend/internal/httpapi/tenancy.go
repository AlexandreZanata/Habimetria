package httpapi

import (
	"context"
	"net/http"

	"vault/internal/store/postgres"
)

type tenancyCreator interface {
	Create(ctx context.Context, propertyID string) (postgres.Tenancy, error)
}

// TenancyHandler serves POST /api/v1/properties/{id}/tenancies.
type TenancyHandler struct {
	svc tenancyCreator
}

func NewTenancyHandler(svc tenancyCreator) *TenancyHandler {
	return &TenancyHandler{svc: svc}
}

func (h *TenancyHandler) Create(w http.ResponseWriter, r *http.Request) {
	t, err := h.svc.Create(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, t)
}
