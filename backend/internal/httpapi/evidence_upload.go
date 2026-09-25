package httpapi

import (
	"net/http"
	"time"

	"vault/internal/service"
)

// MaxMultipartMemory caps in-RAM multipart buffering; rest spills to disk.
const MaxMultipartMemory = 32 << 20

// Upload accepts multipart form: file (bytes, required), type
// (photo|video|doc), room_id + captured_at_device (RFC3339, optional).
// Returns 201 with evidence id + server timestamp ack.
func (h *EvidenceHandler) Upload(w http.ResponseWriter, r *http.Request) {
	inspectionID := r.PathValue("id")
	r.Body = http.MaxBytesReader(w, r.Body, service.MaxVideoBytes+ (1 << 20))
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

	in := service.UploadInput{
		InspectionID: inspectionID,
		Type:         r.FormValue("type"),
		Filename:     header.Filename,
		Mime:         header.Header.Get("Content-Type"),
		Content:      file,
	}
	if v := r.FormValue("room_id"); v != "" {
		roomID := v
		in.RoomID = &roomID
	}
	if v := r.FormValue("captured_at_device"); v != "" {
		ts, err := time.Parse(time.RFC3339, v)
		if err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "captured_at_device must be RFC3339"})
			return
		}
		in.CapturedAtDevice = &ts
	}
	if in.Mime == "" {
		in.Mime = "application/octet-stream"
	}
	out, err := h.svc.Upload(r.Context(), in)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, out.Evidence)
}
