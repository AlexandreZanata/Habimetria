package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ComparisonInput carries one user-confirmed tag for upsert.
type ComparisonInput struct {
	TenancyID       string
	EntryRoomID     string
	ExitRoomID      string
	ShotCategory    string
	EntryEvidenceID *string
	ExitEvidenceID  *string
	Tag             string
}

// UpsertComparison records one tag per exit position. Retags overwrite
// the row; history stays on device. Rooms must belong to the tenancy's
// entry/exit inspections (entry room on kind=entry, exit room on
// kind=exit), otherwise ErrMismatch. Unknown rows map to ErrNotFound.
func UpsertComparison(ctx context.Context, pool *pgxpool.Pool, in ComparisonInput) (Comparison, error) {
	var entryKind, entryTenancy, exitKind, exitTenancy string
	err := pool.QueryRow(ctx,
		`SELECT ri.kind, ri.tenancy_id::text, re.kind, re.tenancy_id::text
		 FROM rooms rentry
		 JOIN inspections ri ON ri.id = rentry.inspection_id
		 JOIN rooms rexit ON rexit.id = $2
		 JOIN inspections re ON re.id = rexit.inspection_id
		 WHERE rentry.id = $1`,
		in.EntryRoomID, in.ExitRoomID,
	).Scan(&entryKind, &entryTenancy, &exitKind, &exitTenancy)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Comparison{}, fmt.Errorf("rooms %s/%s: %w", in.EntryRoomID, in.ExitRoomID, ErrNotFound)
		}
		return Comparison{}, fmt.Errorf("check rooms: %w", err)
	}
	if entryTenancy != in.TenancyID || exitTenancy != in.TenancyID ||
		entryKind != "entry" || exitKind != "exit" {
		return Comparison{}, fmt.Errorf("rooms vs tenancy %s: %w", in.TenancyID, ErrMismatch)
	}
	for _, ev := range []*string{in.EntryEvidenceID, in.ExitEvidenceID} {
		if ev == nil {
			continue
		}
		var one int
		if err := pool.QueryRow(ctx, `SELECT 1 FROM evidences WHERE id=$1`, *ev).Scan(&one); err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return Comparison{}, fmt.Errorf("evidence %s: %w", *ev, ErrNotFound)
			}
			return Comparison{}, fmt.Errorf("check evidence: %w", err)
		}
	}

	var c Comparison
	err = pool.QueryRow(ctx,
		`INSERT INTO comparisons (tenancy_id, entry_room_id, exit_room_id, shot_category,
		                          entry_evidence_id, exit_evidence_id, tag)
		 VALUES ($1, $2, $3, $4, $5, $6, $7)
		 ON CONFLICT (exit_room_id, shot_category)
		 DO UPDATE SET entry_room_id = EXCLUDED.entry_room_id,
		               entry_evidence_id = EXCLUDED.entry_evidence_id,
		               exit_evidence_id = EXCLUDED.exit_evidence_id,
		               tag = EXCLUDED.tag
		 RETURNING id::text, tenancy_id::text, entry_room_id::text, exit_room_id::text,
		           shot_category, entry_evidence_id::text, exit_evidence_id::text, tag, created_at`,
		in.TenancyID, in.EntryRoomID, in.ExitRoomID, in.ShotCategory,
		in.EntryEvidenceID, in.ExitEvidenceID, in.Tag,
	).Scan(&c.ID, &c.TenancyID, &c.EntryRoomID, &c.ExitRoomID,
		&c.ShotCategory, &c.EntryEvidenceID, &c.ExitEvidenceID, &c.Tag, &c.CreatedAt)
	if err != nil {
		return Comparison{}, fmt.Errorf("upsert comparison: %w", err)
	}
	return c, nil
}

// ListComparisons returns a tenancy's tags, oldest first.
func ListComparisons(ctx context.Context, pool *pgxpool.Pool, tenancyID string) ([]Comparison, error) {
	rows, err := pool.Query(ctx,
		`SELECT id::text, tenancy_id::text, entry_room_id::text, exit_room_id::text,
		        shot_category, entry_evidence_id::text, exit_evidence_id::text, tag, created_at
		 FROM comparisons WHERE tenancy_id=$1 ORDER BY created_at`,
		tenancyID,
	)
	if err != nil {
		return nil, fmt.Errorf("list comparisons: %w", err)
	}
	defer rows.Close()
	out := []Comparison{}
	for rows.Next() {
		var c Comparison
		if err := rows.Scan(&c.ID, &c.TenancyID, &c.EntryRoomID, &c.ExitRoomID,
			&c.ShotCategory, &c.EntryEvidenceID, &c.ExitEvidenceID, &c.Tag, &c.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan comparison: %w", err)
		}
		out = append(out, c)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate comparisons: %w", err)
	}
	return out, nil
}
