package httpapi

import (
	"net/http"
)

// Open serves the public share view: GET /s/{token}.
// Logs accessed on every hit. No auth, no photos, no raw keys.
func (h *ShareHandler) Open(w http.ResponseWriter, r *http.Request) {
	view, err := h.svc.Open(r.Context(), r.PathValue("token"), clientIP(r))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, view)
}

// DownloadPDF redirects to a fresh signed PDF URL: GET /s/{token}/pdf.
// Logs downloaded. 404 when unknown, 400 when the PDF is not attached.
func (h *ShareHandler) DownloadPDF(w http.ResponseWriter, r *http.Request) {
	url, err := h.svc.DownloadURL(r.Context(), r.PathValue("token"), clientIP(r))
	if err != nil {
		writeError(w, err)
		return
	}
	http.Redirect(w, r, url, http.StatusFound)
}
