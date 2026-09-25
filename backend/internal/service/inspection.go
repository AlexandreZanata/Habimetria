package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// InspectionService creates DRAFT inspections under a tenancy.
type InspectionService struct {
	pool *pgxpool.Pool
}

// NewInspection returns an InspectionService bound to the pool.
func NewInspection(pool *pgxpool.Pool) *InspectionService {
	return &InspectionService{pool: pool}
}

// Create validates ids and inserts a DRAFT inspection. Empty kind means entry.
func (s *InspectionService) Create(ctx context.Context, tenancyID, kind string) (postgres.Inspection, error) {
	if _, err := uuid.Parse(tenancyID); err != nil {
		return postgres.Inspection{}, fmt.Errorf("tenancy_id: %w", ErrInvalid)
	}
	kind = strings.ToLower(strings.TrimSpace(kind))
	if kind == "" {
		kind = "entry"
	}
	if kind != "entry" && kind != "exit" {
		return postgres.Inspection{}, fmt.Errorf("kind must be entry|exit: %w", ErrInvalid)
	}
	in, err := postgres.CreateInspection(ctx, s.pool, tenancyID, kind)
	if err != nil {
		return postgres.Inspection{}, err
	}
	return in, nil
}
