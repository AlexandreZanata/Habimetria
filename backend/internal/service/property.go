package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// PropertyService creates unclaimed Properties for the explicit development API.
type PropertyService struct {
	pool *pgxpool.Pool
}

// NewProperty returns a PropertyService bound to the pool.
func NewProperty(pool *pgxpool.Pool) *PropertyService {
	return &PropertyService{pool: pool}
}

// Create validates and inserts a property.
func (s *PropertyService) Create(ctx context.Context, address string) (postgres.Property, error) {
	address = strings.TrimSpace(address)
	if len(address) < 3 {
		return postgres.Property{}, fmt.Errorf("address: %w", ErrInvalid)
	}
	p, err := postgres.CreateProperty(ctx, s.pool, address)
	if err != nil {
		return postgres.Property{}, err
	}
	return p, nil
}
