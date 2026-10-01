package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateOccurrence inserts a timeline entry on a property.
// Unknown property maps to ErrNotFound (404).
func CreateOccurrence(ctx context.Context, pool *pgxpool.Pool, propertyID, title string, body *string) (Occurrence, error) {
	var o Occurrence
	err := pool.QueryRow(ctx,
		`INSERT INTO occurrences (property_id, title, body) VALUES ($1, $2, $3)
		 RETURNING id::text, property_id::text, title, body, created_at`,
		propertyID, title, body,
	).Scan(&o.ID, &o.PropertyID, &o.Title, &o.Body, &o.CreatedAt)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23503" {
			return Occurrence{}, fmt.Errorf("property %s: %w", propertyID, ErrNotFound)
		}
		return Occurrence{}, fmt.Errorf("insert occurrence: %w", err)
	}
	return o, nil
}

// ListOccurrences returns a property timeline, newest first.
func ListOccurrences(ctx context.Context, pool *pgxpool.Pool, propertyID string) ([]Occurrence, error) {
	rows, err := pool.Query(ctx,
		`SELECT id::text, property_id::text, title, body, created_at
		 FROM occurrences WHERE property_id=$1 ORDER BY created_at DESC`,
		propertyID,
	)
	if err != nil {
		return nil, fmt.Errorf("list occurrences: %w", err)
	}
	defer rows.Close()
	out := []Occurrence{}
	for rows.Next() {
		var o Occurrence
		if err := rows.Scan(&o.ID, &o.PropertyID, &o.Title, &o.Body, &o.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan occurrence: %w", err)
		}
		out = append(out, o)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate occurrences: %w", err)
	}
	return out, nil
}

// PropertyExists reports whether a property row exists.
func PropertyExists(ctx context.Context, pool *pgxpool.Pool, propertyID string) (bool, error) {
	var one int
	err := pool.QueryRow(ctx, `SELECT 1 FROM properties WHERE id=$1`, propertyID).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("check property: %w", err)
	}
	return true, nil
}
