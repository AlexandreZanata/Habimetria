package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type propertyCreator interface {
	Create(ctx context.Context, address string) (postgres.Property, error)
}

// PropertyHandler serves POST /api/v1/properties.
type PropertyHandler struct {
	svc propertyCreator
}

func NewPropertyHandler(svc propertyCreator) *PropertyHandler {
	return &PropertyHandler{svc: svc}
}

func (h *PropertyHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Address string `json:"address"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	p, err := h.svc.Create(r.Context(), body.Address)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, p)
}
