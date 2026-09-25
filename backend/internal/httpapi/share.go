package httpapi

import (
	"context"
	"encoding/json"
	"net"
	"net/http"
	"strings"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type shareService interface {
	Create(ctx context.Context, reportID, channel, ip string) (postgres.Share, error)
	Timeline(ctx context.Context, reportID string) ([]postgres.ShareTimeline, error)
	Open(ctx context.Context, token, ip string) (service.ShareView, error)
	DownloadURL(ctx context.Context, token, ip string) (string, error)
	ShareURL(token string) string
}

// ShareHandler serves share creation, timeline, and the public view.
type ShareHandler struct {
	svc shareService
}

func NewShareHandler(svc shareService) *ShareHandler {
	return &ShareHandler{svc: svc}
}

func (h *ShareHandler) Create(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Channel string `json:"channel"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
		return
	}
	share, err := h.svc.Create(r.Context(), r.PathValue("id"), body.Channel, clientIP(r))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{
		"share": share,
		"url":   h.svc.ShareURL(share.Token),
	})
}

func (h *ShareHandler) Timeline(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.Timeline(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}

// clientIP prefers the first X-Forwarded-For entry, else RemoteAddr.
// Logged only as an HMAC hash; see service.hashIP.
func clientIP(r *http.Request) string {
	if fwd := r.Header.Get("X-Forwarded-For"); fwd != "" {
		return strings.TrimSpace(strings.Split(fwd, ",")[0])
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}
