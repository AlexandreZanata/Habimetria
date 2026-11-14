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
// kind=exit), otherwise ErrMismatch. Evidence must belong to its selected
// Room/Inspection; unknown and out-of-scope references map to ErrNotFound.
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
	for _, ref := range []struct {
		evidence *string
		room     string
	}{
		{in.EntryEvidenceID, in.EntryRoomID}, {in.ExitEvidenceID, in.ExitRoomID},
	} {
		if ref.evidence == nil {
			continue
		}
		var one int
		if err := pool.QueryRow(ctx, `SELECT 1 FROM evidences e JOIN rooms r ON r.id=e.room_id
			WHERE e.id=$1 AND r.id=$2 AND e.inspection_id=r.inspection_id`, *ref.evidence, ref.room).Scan(&one); err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return Comparison{}, fmt.Errorf("comparison evidence outside selected room: %w", ErrNotFound)
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
		`SELECT c.id::text, c.tenancy_id::text, c.entry_room_id::text, c.exit_room_id::text,
		        c.shot_category, c.entry_evidence_id::text, c.exit_evidence_id::text, c.tag, c.created_at,
		        COALESCE(ei.tenancy_id=c.tenancy_id AND xi.tenancy_id=c.tenancy_id AND ei.kind='entry' AND xi.kind='exit'
		        AND (c.entry_evidence_id IS NULL OR (ee.room_id=er.id AND ee.inspection_id=ei.id))
		        AND (c.exit_evidence_id IS NULL OR (xe.room_id=xr.id AND xe.inspection_id=xi.id)), false)
		 FROM comparisons c LEFT JOIN rooms er ON er.id=c.entry_room_id LEFT JOIN inspections ei ON ei.id=er.inspection_id
		 LEFT JOIN rooms xr ON xr.id=c.exit_room_id LEFT JOIN inspections xi ON xi.id=xr.inspection_id
		 LEFT JOIN evidences ee ON ee.id=c.entry_evidence_id LEFT JOIN evidences xe ON xe.id=c.exit_evidence_id
		 WHERE c.tenancy_id=$1 ORDER BY c.created_at`,
		tenancyID,
	)
	if err != nil {
		return nil, fmt.Errorf("list comparisons: %w", err)
	}
	defer rows.Close()
	out := []Comparison{}
	for rows.Next() {
		var c Comparison
		var scoped bool
		if err := rows.Scan(&c.ID, &c.TenancyID, &c.EntryRoomID, &c.ExitRoomID,
			&c.ShotCategory, &c.EntryEvidenceID, &c.ExitEvidenceID, &c.Tag, &c.CreatedAt, &scoped); err != nil {
			return nil, fmt.Errorf("scan comparison: %w", err)
		}
		if !scoped {
			return nil, fmt.Errorf("comparison references outside tenancy: %w", ErrNotFound)
		}
		out = append(out, c)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate comparisons: %w", err)
	}
	return out, nil
}
