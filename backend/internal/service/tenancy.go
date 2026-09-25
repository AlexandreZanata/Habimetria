package service

import (
	"context"
	"fmt"

	"vault/internal/store/postgres"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// TenancyService creates tenancies under a property.
type TenancyService struct {
	pool *pgxpool.Pool
}

// NewTenancy returns a TenancyService bound to the pool.
func NewTenancy(pool *pgxpool.Pool) *TenancyService {
	return &TenancyService{pool: pool}
}

// Create validates the property id and inserts an entry tenancy.
func (s *TenancyService) Create(ctx context.Context, propertyID string) (postgres.Tenancy, error) {
	if _, err := uuid.Parse(propertyID); err != nil {
		return postgres.Tenancy{}, fmt.Errorf("property_id: %w", ErrInvalid)
	}
	t, err := postgres.CreateTenancy(ctx, s.pool, propertyID)
	if err != nil {
		return postgres.Tenancy{}, err
	}
	return t, nil
}
