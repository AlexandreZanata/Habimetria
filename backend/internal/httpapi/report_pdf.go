package httpapi

import (
	"net/http"

	"vault/internal/service"
)

// Attach accepts the final app-rendered PDF (multipart field `file`).
// Returns 201 with report_sha256 once stored and recorded.
func (h *ReportHandler) Attach(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, service.MaxPDFBytes+(1<<20))
	if err := r.ParseMultipartForm(MaxMultipartMemory); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid multipart body"})
		return
	}
	file, header, err := r.FormFile("file")
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "field file is required"})
		return
	}
	defer file.Close()
	rep, err := h.svc.Attach(r.Context(), service.AttachInput{
		ReportID: r.PathValue("id"),
		Filename: header.Filename,
		Mime:     header.Header.Get("Content-Type"),
		Content:  file,
	})
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, rep)
}

// DownloadURL returns a 5-minute signed URL for the final PDF.
func (h *ReportHandler) DownloadURL(w http.ResponseWriter, r *http.Request) {
	url, expiresAt, err := h.svc.DownloadURL(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"url": url, "expires_at": expiresAt.UTC().Format("2006-01-02T15:04:05Z")})
}
