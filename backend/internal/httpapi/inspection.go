package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type inspectionCreator interface {
	Create(ctx context.Context, tenancyID, kind string) (postgres.Inspection, error)
}

// InspectionHandler serves POST /api/v1/tenancies/{id}/inspections.
type InspectionHandler struct {
	svc inspectionCreator
}

func NewInspectionHandler(svc inspectionCreator) *InspectionHandler {
	return &InspectionHandler{svc: svc}
}

func (h *InspectionHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Kind string `json:"kind"`
	}
	if r.ContentLength != 0 {
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
			return
		}
	}
	in, err := h.svc.Create(r.Context(), r.PathValue("id"), body.Kind)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, in)
}
