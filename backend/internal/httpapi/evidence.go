package httpapi

import (
	"context"
	"time"

	"vault/internal/service"
)

// evidenceUploader is the upload/download surface the handler needs.
type evidenceUploader interface {
	Upload(ctx context.Context, in service.UploadInput) (service.UploadResult, error)
	DownloadURL(ctx context.Context, inspectionID, evidenceID string) (string, time.Time, error)
}

// EvidenceHandler serves upload + signed download URLs.
type EvidenceHandler struct {
	svc evidenceUploader
}

func NewEvidenceHandler(svc evidenceUploader) *EvidenceHandler {
	return &EvidenceHandler{svc: svc}
}
