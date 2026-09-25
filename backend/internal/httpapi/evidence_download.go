package httpapi

import (
	"net/http"
)

// DownloadURL returns a 5-minute signed GET URL for one evidence.
// Query: ?evidence_id={uuid}. The raw bucket key never leaves the server.
func (h *EvidenceHandler) DownloadURL(w http.ResponseWriter, r *http.Request) {
	evidenceID := r.URL.Query().Get("evidence_id")
	if evidenceID == "" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "evidence_id is required"})
		return
	}
	url, expiresAt, err := h.svc.DownloadURL(r.Context(), r.PathValue("id"), evidenceID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"url": url, "expires_at": expiresAt.UTC().Format("2006-01-02T15:04:05Z")})
}
