package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type findingService interface {
	Create(ctx context.Context, roomID, evidenceID, preset string, note *string) (postgres.Finding, error)
	List(ctx context.Context, roomID string) ([]postgres.Finding, error)
}

// FindingHandler serves POST + GET /api/v1/rooms/{id}/findings.
type FindingHandler struct {
	svc findingService
}

func NewFindingHandler(svc findingService) *FindingHandler {
	return &FindingHandler{svc: svc}
}

func (h *FindingHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		EvidenceID string  `json:"evidence_id"`
		Preset     string  `json:"preset"`
		Note       *string `json:"note"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	f, err := h.svc.Create(r.Context(), r.PathValue("id"), body.EvidenceID, body.Preset, body.Note)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, f)
}

func (h *FindingHandler) List(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.List(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
