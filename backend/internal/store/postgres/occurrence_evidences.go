package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// LinkOccurrenceEvidence attaches one photo evidence as proof on an
// occurrence. The evidence must belong to the occurrence's property,
// otherwise ErrMismatch. Re-linking the same pair is ErrAlreadyLinked.
// Linking never mutates inspections.
func LinkOccurrenceEvidence(ctx context.Context, pool *pgxpool.Pool, occurrenceID, evidenceID string) (OccurrenceEvidence, error) {
	var propertyID string
	err := pool.QueryRow(ctx,
		`SELECT property_id::text FROM occurrences WHERE id=$1`, occurrenceID,
	).Scan(&propertyID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return OccurrenceEvidence{}, fmt.Errorf("occurrence %s: %w", occurrenceID, ErrNotFound)
		}
		return OccurrenceEvidence{}, fmt.Errorf("check occurrence: %w", err)
	}

	var evidenceProperty string
	err = pool.QueryRow(ctx,
		`SELECT t.property_id::text
		 FROM evidences e
		 JOIN inspections i ON i.id = e.inspection_id
		 JOIN tenancies t ON t.id = i.tenancy_id
		 WHERE e.id=$1`, evidenceID,
	).Scan(&evidenceProperty)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return OccurrenceEvidence{}, fmt.Errorf("evidence %s: %w", evidenceID, ErrNotFound)
		}
		return OccurrenceEvidence{}, fmt.Errorf("check evidence: %w", err)
	}
	if evidenceProperty != propertyID {
		return OccurrenceEvidence{}, fmt.Errorf(
			"evidence %s vs occurrence %s: %w", evidenceID, occurrenceID, ErrMismatch,
		)
	}

	var link OccurrenceEvidence
	err = pool.QueryRow(ctx,
		`INSERT INTO occurrence_evidences (occurrence_id, evidence_id) VALUES ($1, $2)
		 RETURNING occurrence_id::text, evidence_id::text`,
		occurrenceID, evidenceID,
	).Scan(&link.OccurrenceID, &link.EvidenceID)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return OccurrenceEvidence{}, fmt.Errorf(
				"evidence %s vs occurrence %s: %w", evidenceID, occurrenceID, ErrAlreadyLinked,
			)
		}
		return OccurrenceEvidence{}, fmt.Errorf("link evidence: %w", err)
	}
	return link, nil
}

// ListOccurrenceEvidences returns the photo proof of one occurrence,
// oldest received first. Unknown occurrence maps to ErrNotFound.
func ListOccurrenceEvidences(ctx context.Context, pool *pgxpool.Pool, occurrenceID string) ([]Evidence, error) {
	ok, err := OccurrenceExists(ctx, pool, occurrenceID)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fmt.Errorf("occurrence %s: %w", occurrenceID, ErrNotFound)
	}
	rows, err := pool.Query(ctx,
		`SELECT e.id::text, e.inspection_id::text, e.room_id::text, e.type, e.mime,
		        e.size, e.storage_key, e.sha256, e.captured_at_device,
		        e.latitude, e.longitude, e.received_at_server, e.sequence
		 FROM occurrence_evidences oe
		 JOIN evidences e ON e.id = oe.evidence_id
		 WHERE oe.occurrence_id=$1 ORDER BY e.received_at_server`,
		occurrenceID,
	)
	if err != nil {
		return nil, fmt.Errorf("list occurrence evidences: %w", err)
	}
	defer rows.Close()
	out := []Evidence{}
	for rows.Next() {
		var e Evidence
		if err := rows.Scan(&e.ID, &e.InspectionID, &e.RoomID, &e.Type, &e.Mime,
			&e.Size, &e.StorageKey, &e.SHA256, &e.CapturedAtDevice,
			&e.Latitude, &e.Longitude, &e.ReceivedAtServer, &e.Sequence); err != nil {
			return nil, fmt.Errorf("scan evidence: %w", err)
		}
		out = append(out, e)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate occurrence evidences: %w", err)
	}
	return out, nil
}

// OccurrenceExists reports whether an occurrence row exists.
func OccurrenceExists(ctx context.Context, pool *pgxpool.Pool, occurrenceID string) (bool, error) {
	var one int
	err := pool.QueryRow(ctx, `SELECT 1 FROM occurrences WHERE id=$1`, occurrenceID).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("check occurrence: %w", err)
	}
	return true, nil
}
