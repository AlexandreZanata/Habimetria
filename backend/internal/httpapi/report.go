package httpapi

import (
	"context"
	"net/http"
	"time"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type reportService interface {
	Detail(ctx context.Context, reportID string) (postgres.Report, string, error)
	Attach(ctx context.Context, in service.AttachInput) (postgres.Report, error)
	DownloadURL(ctx context.Context, reportID string) (string, time.Time, error)
}

// ReportHandler serves report detail, PDF attach, and PDF download URL.
type ReportHandler struct {
	svc reportService
}

func NewReportHandler(svc reportService) *ReportHandler {
	return &ReportHandler{svc: svc}
}

// Detail returns the report with the QR-ready verify URL.
func (h *ReportHandler) Detail(w http.ResponseWriter, r *http.Request) {
	rep, verifyURL, err := h.svc.Detail(r.Context(), r.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"report": rep, "verify_url": verifyURL})
}
