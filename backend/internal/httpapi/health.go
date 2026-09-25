package httpapi

import (
	"net/http"

	"vault/internal/service"
)

// HealthHandler serves GET /healthz (always 200) and GET /readyz (200/503).
type HealthHandler struct {
	health *service.HealthService
}

// NewHealthHandler wires the health service. No DB access here.
func NewHealthHandler(h *service.HealthService) *HealthHandler {
	return &HealthHandler{health: h}
}

func (h *HealthHandler) Live(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, h.health.Check(r.Context()))
}

func (h *HealthHandler) Ready(w http.ResponseWriter, r *http.Request) {
	if !h.health.Ready(r.Context()) {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "not_ready"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}
