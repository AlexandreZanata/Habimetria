package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateTenancy inserts an entry tenancy for a property.
// Returns ErrNotFound when the property does not exist.
func CreateTenancy(ctx context.Context, pool *pgxpool.Pool, propertyID string) (Tenancy, error) {
	var t Tenancy
	err := pool.QueryRow(ctx,
		`INSERT INTO tenancies (property_id) VALUES ($1)
		 RETURNING id::text, property_id::text, type, created_at`,
		propertyID,
	).Scan(&t.ID, &t.PropertyID, &t.Type, &t.CreatedAt)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23503" {
			return Tenancy{}, fmt.Errorf("property %s: %w", propertyID, ErrNotFound)
		}
		if errors.Is(err, pgx.ErrNoRows) {
			return Tenancy{}, ErrNotFound
		}
		return Tenancy{}, fmt.Errorf("insert tenancy: %w", err)
	}
	return t, nil
}
