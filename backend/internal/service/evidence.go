package service

import (
	"context"
	"errors"
	"io"
	"time"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// Tunable upload limits. Vars (not consts) so tests can shrink them.
var (
	MaxPhotoBytes = int64(25 << 20)  // 25 MB
	MaxVideoBytes = int64(100 << 20) // 100 MB
	MaxDocBytes   = int64(25 << 20)  // 25 MB
	DownloadTTL   = 5 * time.Minute
)

// ErrTooLarge marks a 413 file-size rejection.
var ErrTooLarge = errors.New("file too large")

// ErrQuota marks a 422 tenancy-cap rejection.
var ErrQuota = errors.New("tenancy quota exceeded")

// EvidenceService seals bytes into immutable originals.
type EvidenceService struct {
	pool    *pgxpool.Pool
	store   EvidenceStore
	storage EvidenceStorage
}

// EvidenceStore is the SQL surface Upload needs. *pgxpool-backed
// adapters live in store/postgres; tests use fakes.
type EvidenceStore interface {
	UploadContext(ctx context.Context, inspectionID string) (postgres.UploadContext, error)
	Create(ctx context.Context, in postgres.EvidenceInput) (postgres.Evidence, error)
	Get(ctx context.Context, inspectionID, evidenceID string) (postgres.Evidence, error)
	RoomBelongsTo(ctx context.Context, inspectionID, roomID string) (bool, error)
}

// EvidenceStorage is the object-storage surface Upload needs.
type EvidenceStorage interface {
	PutOriginal(ctx context.Context, key string, r io.Reader, mime string) error
	SignedGetURL(ctx context.Context, key string, ttl time.Duration) (string, error)
	Delete(ctx context.Context, key string) error
}

// UploadInput carries one file plus client metadata. Hash and
// server timestamp are always computed server-side. Geolocation and
// device time are device-reported claims.
type UploadInput struct {
	InspectionID     string
	RoomID           *string
	Type             string // photo | video | doc
	Filename         string
	Mime             string
	CapturedAtDevice *time.Time
	Latitude         *float64
	Longitude        *float64
	Content          io.Reader
}

// UploadResult is the client ack: id + server timestamp.
type UploadResult struct {
	Evidence postgres.Evidence `json:"evidence"`
}
