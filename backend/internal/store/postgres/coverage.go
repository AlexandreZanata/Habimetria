package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// RoomCoverageRow is one room plus its photo count for coverage math.
type RoomCoverageRow struct {
	RoomID string
	Key    string
	Label  string
	Sort   int
	Photos int
}

// GetCoverage returns rooms of an inspection with photo counts.
// Uploads without room_id count toward the inspection, not a room.
// Returns ErrNotFound when the inspection does not exist.
func GetCoverage(ctx context.Context, pool *pgxpool.Pool, inspectionID string) ([]RoomCoverageRow, error) {
	var one int
	err := pool.QueryRow(ctx, `SELECT 1 FROM inspections WHERE id=$1`, inspectionID).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
		}
		return nil, fmt.Errorf("check inspection: %w", err)
	}
	rows, err := pool.Query(ctx,
		`SELECT r.id::text, r.key, r.label, r.sort,
		        COUNT(e.id) FILTER (WHERE e.type='photo')
		 FROM rooms r
		 LEFT JOIN evidences e ON e.room_id = r.id AND e.inspection_id = r.inspection_id
		 WHERE r.inspection_id=$1
		 GROUP BY r.id, r.key, r.label, r.sort
		 ORDER BY r.sort, r.key`,
		inspectionID,
	)
	if err != nil {
		return nil, fmt.Errorf("coverage query: %w", err)
	}
	defer rows.Close()
	out := []RoomCoverageRow{}
	for rows.Next() {
		var c RoomCoverageRow
		if err := rows.Scan(&c.RoomID, &c.Key, &c.Label, &c.Sort, &c.Photos); err != nil {
			return nil, fmt.Errorf("scan coverage: %w", err)
		}
		out = append(out, c)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate coverage: %w", err)
	}
	return out, nil
}
