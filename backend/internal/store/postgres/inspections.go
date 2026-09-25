package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateInspection inserts a DRAFT inspection. kind is entry or exit.
// Returns ErrNotFound when the tenancy does not exist.
func CreateInspection(ctx context.Context, pool *pgxpool.Pool, tenancyID, kind string) (Inspection, error) {
	var in Inspection
	err := pool.QueryRow(ctx,
		`INSERT INTO inspections (tenancy_id, kind) VALUES ($1, $2)
		 RETURNING id::text, tenancy_id::text, kind, status, created_at`,
		tenancyID, kind,
	).Scan(&in.ID, &in.TenancyID, &in.Kind, &in.Status, &in.CreatedAt)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23503" {
			return Inspection{}, fmt.Errorf("tenancy %s: %w", tenancyID, ErrNotFound)
		}
		if errors.Is(err, pgx.ErrNoRows) {
			return Inspection{}, ErrNotFound
		}
		return Inspection{}, fmt.Errorf("insert inspection: %w", err)
	}
	return in, nil
}

// InspectionExists reports whether an inspection row exists.
func InspectionExists(ctx context.Context, pool *pgxpool.Pool, inspectionID string) (bool, error) {
	var one int
	err := pool.QueryRow(ctx, `SELECT 1 FROM inspections WHERE id=$1`, inspectionID).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("check inspection: %w", err)
	}
	return true, nil
}
