package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type compareService interface {
	Upsert(
		ctx context.Context,
		tenancyID, entryRoomID, exitRoomID, shotCategory string,
		entryEvidenceID, exitEvidenceID *string,
		tag string,
	) (postgres.Comparison, error)
	List(ctx context.Context, tenancyID string) ([]postgres.Comparison, error)
}

// CompareHandler serves user-confirmed entry-vs-exit tags. Upserts
// overwrite the server row; tag history stays on device.
type CompareHandler struct {
	svc compareService
}

// NewCompareHandler returns a CompareHandler.
func NewCompareHandler(svc compareService) *CompareHandler {
	return &CompareHandler{svc: svc}
}

func (h *CompareHandler) Upsert(w http.ResponseWriter, r *http.Request) {
	var body struct {
		EntryRoomID     string  `json:"entry_room_id"`
		ExitRoomID      string  `json:"exit_room_id"`
		ShotCategory    string  `json:"shot_category"`
		EntryEvidenceID *string `json:"entry_evidence_id"`
		ExitEvidenceID  *string `json:"exit_evidence_id"`
		Tag             string  `json:"tag"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	c, err := h.svc.Upsert(
		r.Context(), r.PathValue("id"),
		body.EntryRoomID, body.ExitRoomID, body.ShotCategory,
		body.EntryEvidenceID, body.ExitEvidenceID, body.Tag,
	)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, c)
}

func (h *CompareHandler) List(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.List(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
