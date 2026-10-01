package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type occurrenceService interface {
	Create(ctx context.Context, propertyID, title string, body *string) (postgres.Occurrence, error)
	List(ctx context.Context, propertyID string) ([]postgres.Occurrence, error)
}

// OccurrenceHandler serves the property occurrence timeline.
type OccurrenceHandler struct {
	svc occurrenceService
}

// NewOccurrenceHandler returns an OccurrenceHandler.
func NewOccurrenceHandler(svc occurrenceService) *OccurrenceHandler {
	return &OccurrenceHandler{svc: svc}
}

func (h *OccurrenceHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Title string  `json:"title"`
		Body  *string `json:"body"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	o, err := h.svc.Create(r.Context(), r.PathValue("id"), body.Title, body.Body)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, o)
}

func (h *OccurrenceHandler) List(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.List(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
