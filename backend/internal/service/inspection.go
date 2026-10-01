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
// kind=exit mirrors the mobile V1.2 rule on the server: it requires a
// sealed entry inspection and allows only one exit per tenancy (409).
// Unknown tenancy maps to ErrNotFound (404).
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
	if kind == "exit" {
		if err := checkExitAllowed(ctx, s.pool, tenancyID); err != nil {
			return postgres.Inspection{}, err
		}
	}
	in, err := postgres.CreateInspection(ctx, s.pool, tenancyID, kind)
	if err != nil {
		return postgres.Inspection{}, err
	}
	return in, nil
}

// checkExitAllowed enforces the exit preconditions before insert.
func checkExitAllowed(ctx context.Context, pool *pgxpool.Pool, tenancyID string) error {
	exists, err := postgres.TenancyExists(ctx, pool, tenancyID)
	if err != nil {
		return err
	}
	if !exists {
		return fmt.Errorf("tenancy %s: %w", tenancyID, postgres.ErrNotFound)
	}
	prior, err := postgres.ListInspectionsByTenancy(ctx, pool, tenancyID)
	if err != nil {
		return err
	}
	sealedEntry := false
	for _, in := range prior {
		if in.Kind == "exit" {
			return fmt.Errorf("exit already exists: %w", postgres.ErrExitNotAllowed)
		}
		if in.Kind == "entry" && in.Status == "SEALED" {
			sealedEntry = true
		}
	}
	if !sealedEntry {
		return fmt.Errorf("sealed entry required first: %w", postgres.ErrExitNotAllowed)
	}
	return nil
}
