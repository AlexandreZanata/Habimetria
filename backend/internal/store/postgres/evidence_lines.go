package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
)

// EvidenceLine is one manifest line for clients: identity + integrity
// metadata, no bytes. Bytes flow through signed download URLs.
type EvidenceLine struct {
	ID               string  `json:"id"`
	Sequence         int     `json:"sequence"`
	SHA256           string  `json:"sha256"`
	Mime             string  `json:"mime"`
	Size             int64   `json:"size"`
	RoomID           *string `json:"room_id"`
	ReceivedAtServer string  `json:"received_at_server"`
}

// ListEvidences returns the sealed (or draft) evidence set ordered by
// sequence. The PDF renderer consumes this, not the local queue.
func ListEvidences(ctx context.Context, pool *pgxpool.Pool, inspectionID string) ([]EvidenceLine, error) {
	if ok, err := InspectionExists(ctx, pool, inspectionID); err != nil {
		return nil, err
	} else if !ok {
		return nil, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
	}
	rows, err := pool.Query(ctx,
		`SELECT id::text, sequence, sha256, mime, size, room_id::text,
		        to_char(received_at_server, 'YYYY-MM-DD"T"HH24:MI:SSOF"TZH:TZM"')
		 FROM evidences WHERE inspection_id=$1 ORDER BY sequence`,
		inspectionID,
	)
	if err != nil {
		return nil, fmt.Errorf("list evidences: %w", err)
	}
	defer rows.Close()
	out := []EvidenceLine{}
	for rows.Next() {
		var l EvidenceLine
		if err := rows.Scan(&l.ID, &l.Sequence, &l.SHA256, &l.Mime, &l.Size, &l.RoomID, &l.ReceivedAtServer); err != nil {
			return nil, fmt.Errorf("scan evidence: %w", err)
		}
		out = append(out, l)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate evidences: %w", err)
	}
	return out, nil
}
