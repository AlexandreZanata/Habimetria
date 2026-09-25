package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/store/postgres"
)

type roomService interface {
	Create(ctx context.Context, inspectionID, key, label string) (postgres.Room, error)
	List(ctx context.Context, inspectionID string) ([]postgres.Room, error)
	EnsureTemplate(ctx context.Context, inspectionID string) ([]postgres.Room, error)
}

// RoomHandler serves room endpoints under an inspection.
type RoomHandler struct {
	svc roomService
}

func NewRoomHandler(svc roomService) *RoomHandler {
	return &RoomHandler{svc: svc}
}

func (h *RoomHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Key   string `json:"key"`
		Label string `json:"label"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	room, err := h.svc.Create(r.Context(), r.PathValue("id"), body.Key, body.Label)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, room)
}

func (h *RoomHandler) List(w http.ResponseWriter, r *http.Request) {
	rooms, err := h.svc.List(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, rooms)
}

func (h *RoomHandler) EnsureTemplate(w http.ResponseWriter, r *http.Request) {
	rooms, err := h.svc.EnsureTemplate(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, rooms)
}
