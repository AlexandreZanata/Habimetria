package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateFinding inserts a defect report: room + evidence + preset + note.
// The room's inspection is locked and must be DRAFT; the evidence must
// belong to the same inspection, otherwise ErrMismatch.
func CreateFinding(ctx context.Context, pool *pgxpool.Pool, roomID, evidenceID, preset string, note *string) (Finding, error) {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return Finding{}, fmt.Errorf("begin: %w", err)
	}
	defer tx.Rollback(ctx)

	var inspectionID, status string
	err = tx.QueryRow(ctx,
		`SELECT i.id::text, i.status FROM inspections i
		 JOIN rooms r ON r.inspection_id = i.id
		 WHERE r.id = $1 FOR UPDATE OF i`,
		roomID,
	).Scan(&inspectionID, &status)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Finding{}, fmt.Errorf("room %s: %w", roomID, ErrNotFound)
		}
		return Finding{}, fmt.Errorf("lock inspection: %w", err)
	}
	if status != "DRAFT" {
		return Finding{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrSealed)
	}

	var one int
	err = tx.QueryRow(ctx,
		`SELECT 1 FROM evidences WHERE id=$1 AND inspection_id=$2`,
		evidenceID, inspectionID,
	).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Finding{}, fmt.Errorf("evidence %s vs room %s: %w", evidenceID, roomID, ErrMismatch)
		}
		return Finding{}, fmt.Errorf("check evidence: %w", err)
	}

	var f Finding
	err = tx.QueryRow(ctx,
		`INSERT INTO findings (room_id, evidence_id, preset, note)
		 VALUES ($1,$2,$3,$4)
		 RETURNING id::text, room_id::text, evidence_id::text, preset, note, created_at`,
		roomID, evidenceID, preset, note,
	).Scan(&f.ID, &f.RoomID, &f.EvidenceID, &f.Preset, &f.Note, &f.CreatedAt)
	if err != nil {
		return Finding{}, fmt.Errorf("insert finding: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return Finding{}, fmt.Errorf("commit: %w", err)
	}
	return f, nil
}

// ListFindings returns findings of one room, newest last.
func ListFindings(ctx context.Context, pool *pgxpool.Pool, roomID string) ([]Finding, error) {
	rows, err := pool.Query(ctx,
		`SELECT f.id::text, f.room_id::text, f.evidence_id::text, f.preset, f.note, f.created_at
		 FROM findings f JOIN rooms r ON r.id = f.room_id
		 WHERE f.room_id=$1 ORDER BY f.created_at`,
		roomID,
	)
	if err != nil {
		return nil, fmt.Errorf("list findings: %w", err)
	}
	defer rows.Close()
	out := []Finding{}
	for rows.Next() {
		var f Finding
		if err := rows.Scan(&f.ID, &f.RoomID, &f.EvidenceID, &f.Preset, &f.Note, &f.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan finding: %w", err)
		}
		out = append(out, f)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate findings: %w", err)
	}
	return out, nil
}

// RoomExists reports whether a room row exists.
func RoomExists(ctx context.Context, pool *pgxpool.Pool, roomID string) (bool, error) {
	var one int
	err := pool.QueryRow(ctx, `SELECT 1 FROM rooms WHERE id=$1`, roomID).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("check room: %w", err)
	}
	return true, nil
}
