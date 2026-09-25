package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// DefaultRooms is the guided-capture template applied per inspection.
var DefaultRooms = []Room{
	{Key: "living", Label: "Living room", Sort: 1},
	{Key: "kitchen", Label: "Kitchen", Sort: 2},
	{Key: "bedroom", Label: "Bedroom", Sort: 3},
	{Key: "bathroom", Label: "Bathroom", Sort: 4},
	{Key: "exterior", Label: "Exterior", Sort: 5},
}

// CreateRoom inserts one room. Duplicate keys per inspection are rejected.
// Returns ErrNotFound when the inspection does not exist.
func CreateRoom(ctx context.Context, pool *pgxpool.Pool, inspectionID, key, label string) (Room, error) {
	var r Room
	err := pool.QueryRow(ctx,
		`INSERT INTO rooms (inspection_id, key, label, sort)
		 VALUES ($1, $2, $3, COALESCE((SELECT MAX(sort)+1 FROM rooms WHERE inspection_id=$1), 1))
		 RETURNING id::text, inspection_id::text, key, label, sort`,
		inspectionID, key, label,
	).Scan(&r.ID, &r.InspectionID, &r.Key, &r.Label, &r.Sort)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) {
			switch pgErr.Code {
			case "23503":
				return Room{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
			case "23505":
				return Room{}, fmt.Errorf("room %q: %w", key, ErrRoomExists)
			}
		}
		return Room{}, fmt.Errorf("insert room: %w", err)
	}
	return r, nil
}

// ListRooms returns rooms ordered by sort.
func ListRooms(ctx context.Context, pool *pgxpool.Pool, inspectionID string) ([]Room, error) {
	rows, err := pool.Query(ctx,
		`SELECT id::text, inspection_id::text, key, label, sort
		 FROM rooms WHERE inspection_id=$1 ORDER BY sort, key`,
		inspectionID,
	)
	if err != nil {
		return nil, fmt.Errorf("list rooms: %w", err)
	}
	defer rows.Close()
	rooms := []Room{}
	for rows.Next() {
		var r Room
		if err := rows.Scan(&r.ID, &r.InspectionID, &r.Key, &r.Label, &r.Sort); err != nil {
			return nil, fmt.Errorf("scan room: %w", err)
		}
		rooms = append(rooms, r)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate rooms: %w", err)
	}
	return rooms, nil
}

// EnsureTemplate inserts DefaultRooms, skipping existing keys.
// Returns the full ordered list afterwards.
func EnsureTemplate(ctx context.Context, pool *pgxpool.Pool, inspectionID string) ([]Room, error) {
	ok, err := InspectionExists(ctx, pool, inspectionID)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
	}
	for _, d := range DefaultRooms {
		_, err := pool.Exec(ctx,
			`INSERT INTO rooms (inspection_id, key, label, sort)
			 VALUES ($1, $2, $3, $4) ON CONFLICT (inspection_id, key) DO NOTHING`,
			inspectionID, d.Key, d.Label, d.Sort,
		)
		if err != nil {
			var pgErr *pgconn.PgError
			if errors.As(err, &pgErr) && pgErr.Code == "23503" {
				return nil, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
			}
			return nil, fmt.Errorf("ensure room %s: %w", d.Key, err)
		}
	}
	return ListRooms(ctx, pool, inspectionID)
}
