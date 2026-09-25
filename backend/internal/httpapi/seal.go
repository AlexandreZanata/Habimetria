package httpapi

import (
	"context"
	"encoding/json"
	"net/http"

	"vault/internal/service"
)

type sealer interface {
	Seal(ctx context.Context, in service.SealInput) (service.SealOutput, error)
}

// SealHandler serves POST /api/v1/inspections/{id}/seal.
type SealHandler struct {
	svc sealer
}

func NewSealHandler(svc sealer) *SealHandler {
	return &SealHandler{svc: svc}
}

func (h *SealHandler) Seal(w http.ResponseWriter, r *http.Request) {
	var body struct {
		PaymentProof string `json:"payment_proof"`
	}
	if r.ContentLength != 0 {
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid JSON"})
			return
		}
	}
	out, err := h.svc.Seal(r.Context(), service.SealInput{
		InspectionID: r.PathValue("id"),
		PaymentProof: body.PaymentProof,
	})
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, out)
}
