package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateProperty inserts a property row. Address is required.
func CreateProperty(ctx context.Context, pool *pgxpool.Pool, address string) (Property, error) {
	var p Property
	err := pool.QueryRow(ctx,
		`INSERT INTO properties (address) VALUES ($1)
		 RETURNING id::text, address, created_at`,
		address,
	).Scan(&p.ID, &p.Address, &p.CreatedAt)
	if err != nil {
		return Property{}, fmt.Errorf("insert property: %w", err)
	}
	return p, nil
}
