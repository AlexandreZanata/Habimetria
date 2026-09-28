package httpapi

import (
	"net/http"
)

// List returns the evidence set ordered by sequence.
// The PDF renderer consumes this server-side set.
func (h *EvidenceHandler) List(w http.ResponseWriter, r *http.Request) {
	out, err := h.svc.List(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, out)
}
