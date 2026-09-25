package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ErrSealed is returned when writing to a SEALED inspection.
// Originals are immutable after seal; use occurrences instead.
var ErrSealed = errors.New("inspection is sealed")

// UploadContext carries the checks needed before accepting a file.
type UploadContext struct {
	Status     string
	CapPhotos  int
	CapVideos  int
	CapBytes   int64
	PhotoCount int
	VideoCount int
	TotalBytes int64
}

// GetUploadContext loads inspection status plus tenancy caps and usage.
// Returns ErrNotFound when the inspection does not exist.
func GetUploadContext(ctx context.Context, pool *pgxpool.Pool, inspectionID string) (UploadContext, error) {
	var uc UploadContext
	err := pool.QueryRow(ctx,
		`SELECT i.status, t.cap_photos, t.cap_videos, t.cap_bytes,
		        COUNT(e.id) FILTER (WHERE e.type='photo'),
		        COUNT(e.id) FILTER (WHERE e.type='video'),
		        COALESCE(SUM(e.size),0)
		 FROM inspections i
		 JOIN tenancies t ON t.id = i.tenancy_id
		 LEFT JOIN evidences e ON e.inspection_id = i.id
		 WHERE i.id = $1
		 GROUP BY i.status, t.cap_photos, t.cap_videos, t.cap_bytes`,
		inspectionID,
	).Scan(&uc.Status, &uc.CapPhotos, &uc.CapVideos, &uc.CapBytes,
		&uc.PhotoCount, &uc.VideoCount, &uc.TotalBytes)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return UploadContext{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
		}
		return UploadContext{}, fmt.Errorf("upload context: %w", err)
	}
	return uc, nil
}

// EvidenceInput is the server-computed row for one uploaded file.
type EvidenceInput struct {
	ID               string
	InspectionID     string
	RoomID           *string
	Type             string
	Mime             string
	Size             int64
	StorageKey       string
	SHA256           string
	CapturedAtDevice *time.Time
	Latitude         *float64
	Longitude        *float64
}

// CreateEvidence inserts the row in a transaction: locks the inspection,
// re-checks DRAFT (seal race), assigns the next sequence, returns the row
// with the database timestamp as received_at_server.
func CreateEvidence(ctx context.Context, pool *pgxpool.Pool, in EvidenceInput) (Evidence, error) {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return Evidence{}, fmt.Errorf("begin: %w", err)
	}
	defer tx.Rollback(ctx)

	var status string
	if err := tx.QueryRow(ctx, `SELECT status FROM inspections WHERE id=$1 FOR UPDATE`, in.InspectionID).Scan(&status); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Evidence{}, fmt.Errorf("inspection %s: %w", in.InspectionID, ErrNotFound)
		}
		return Evidence{}, fmt.Errorf("lock inspection: %w", err)
	}
	if status != "DRAFT" {
		return Evidence{}, fmt.Errorf("inspection %s: %w", in.InspectionID, ErrSealed)
	}

	var seq int
	if err := tx.QueryRow(ctx, `SELECT COALESCE(MAX(sequence),0)+1 FROM evidences WHERE inspection_id=$1`, in.InspectionID).Scan(&seq); err != nil {
		return Evidence{}, fmt.Errorf("next sequence: %w", err)
	}

	var e Evidence
	err = tx.QueryRow(ctx,
		`INSERT INTO evidences (id, inspection_id, room_id, type, mime, size, storage_key, sha256, captured_at_device, latitude, longitude, sequence)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
		 RETURNING id::text, inspection_id::text, room_id::text, type, mime, size,
		           storage_key, sha256, captured_at_device, latitude, longitude, received_at_server, sequence`,
		in.ID, in.InspectionID, in.RoomID, in.Type, in.Mime, in.Size,
		in.StorageKey, in.SHA256, in.CapturedAtDevice, in.Latitude, in.Longitude, seq,
	).Scan(&e.ID, &e.InspectionID, &e.RoomID, &e.Type, &e.Mime, &e.Size,
		&e.StorageKey, &e.SHA256, &e.CapturedAtDevice, &e.Latitude, &e.Longitude, &e.ReceivedAtServer, &e.Sequence)
	if err != nil {
		return Evidence{}, fmt.Errorf("insert evidence: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return Evidence{}, fmt.Errorf("commit: %w", err)
	}
	return e, nil
}

// GetEvidence returns one evidence scoped to its inspection.
func GetEvidence(ctx context.Context, pool *pgxpool.Pool, inspectionID, evidenceID string) (Evidence, error) {
	var e Evidence
	err := pool.QueryRow(ctx,
		`SELECT id::text, inspection_id::text, room_id::text, type, mime, size,
		        storage_key, sha256, captured_at_device, latitude, longitude, received_at_server, sequence
		 FROM evidences WHERE id=$1 AND inspection_id=$2`,
		evidenceID, inspectionID,
	).Scan(&e.ID, &e.InspectionID, &e.RoomID, &e.Type, &e.Mime, &e.Size,
		&e.StorageKey, &e.SHA256, &e.CapturedAtDevice, &e.Latitude, &e.Longitude, &e.ReceivedAtServer, &e.Sequence)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Evidence{}, fmt.Errorf("evidence %s: %w", evidenceID, ErrNotFound)
		}
		return Evidence{}, fmt.Errorf("get evidence: %w", err)
	}
	return e, nil
}
