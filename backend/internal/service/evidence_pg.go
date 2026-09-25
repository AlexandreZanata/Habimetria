package service

import (
	"context"
	"fmt"
	"time"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// pgEvidenceStore adapts package-level postgres funcs to EvidenceStore.
type pgEvidenceStore struct {
	pool *pgxpool.Pool
}

func (s pgEvidenceStore) UploadContext(ctx context.Context, inspectionID string) (postgres.UploadContext, error) {
	return postgres.GetUploadContext(ctx, s.pool, inspectionID)
}

func (s pgEvidenceStore) Create(ctx context.Context, in postgres.EvidenceInput) (postgres.Evidence, error) {
	return postgres.CreateEvidence(ctx, s.pool, in)
}

func (s pgEvidenceStore) Get(ctx context.Context, inspectionID, evidenceID string) (postgres.Evidence, error) {
	return postgres.GetEvidence(ctx, s.pool, inspectionID, evidenceID)
}

func (s pgEvidenceStore) RoomBelongsTo(ctx context.Context, inspectionID, roomID string) (bool, error) {
	return postgres.RoomBelongsTo(ctx, s.pool, inspectionID, roomID)
}

// NewEvidence returns an EvidenceService backed by Postgres + storage.
func NewEvidence(pool *pgxpool.Pool, storage EvidenceStorage) *EvidenceService {
	return &EvidenceService{pool: pool, store: pgEvidenceStore{pool: pool}, storage: storage}
}

// NewEvidenceWithStore injects fakes. Tests only.
func NewEvidenceWithStore(pool *pgxpool.Pool, store EvidenceStore, storage EvidenceStorage) *EvidenceService {
	return &EvidenceService{pool: pool, store: store, storage: storage}
}

// DownloadURL returns a 5-minute signed URL for one evidence.
func (s *EvidenceService) DownloadURL(ctx context.Context, inspectionID, evidenceID string) (string, time.Time, error) {
	if err := checkUUID(inspectionID, "inspection_id"); err != nil {
		return "", time.Time{}, err
	}
	if err := checkUUID(evidenceID, "evidence_id"); err != nil {
		return "", time.Time{}, err
	}
	e, err := s.store.Get(ctx, inspectionID, evidenceID)
	if err != nil {
		return "", time.Time{}, err
	}
	url, err := s.storage.SignedGetURL(ctx, e.StorageKey, DownloadTTL)
	if err != nil {
		return "", time.Time{}, fmt.Errorf("sign url: %w", err)
	}
	return url, time.Now().Add(DownloadTTL), nil
}
