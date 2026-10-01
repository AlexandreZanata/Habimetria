package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// MaxOccurrenceTitle mirrors the mobile cap: short labels, details in body.
const MaxOccurrenceTitle = 120

// OccurrenceService records post-seal timeline entries on a property.
// Occurrences never mutate inspections.
type OccurrenceService struct {
	pool *pgxpool.Pool
}

// NewOccurrence returns an OccurrenceService bound to the pool.
func NewOccurrence(pool *pgxpool.Pool) *OccurrenceService {
	return &OccurrenceService{pool: pool}
}

// Create validates and inserts an occurrence. Unknown property maps to
// ErrNotFound (404).
func (s *OccurrenceService) Create(ctx context.Context, propertyID, title string, body *string) (postgres.Occurrence, error) {
	if err := checkUUID(propertyID, "property_id"); err != nil {
		return postgres.Occurrence{}, err
	}
	title = strings.TrimSpace(title)
	if title == "" {
		return postgres.Occurrence{}, fmt.Errorf("title: %w", ErrInvalid)
	}
	if len(title) > MaxOccurrenceTitle {
		return postgres.Occurrence{}, fmt.Errorf("title over %d chars: %w", MaxOccurrenceTitle, ErrInvalid)
	}
	var cleanBody *string
	if body != nil && strings.TrimSpace(*body) != "" {
		trimmed := strings.TrimSpace(*body)
		cleanBody = &trimmed
	}
	return postgres.CreateOccurrence(ctx, s.pool, propertyID, title, cleanBody)
}

// List returns a property timeline, newest first. Unknown property maps
// to ErrNotFound (404).
func (s *OccurrenceService) List(ctx context.Context, propertyID string) ([]postgres.Occurrence, error) {
	if err := checkUUID(propertyID, "property_id"); err != nil {
		return nil, err
	}
	exists, err := postgres.PropertyExists(ctx, s.pool, propertyID)
	if err != nil {
		return nil, err
	}
	if !exists {
		return nil, fmt.Errorf("property %s: %w", propertyID, postgres.ErrNotFound)
	}
	return postgres.ListOccurrences(ctx, s.pool, propertyID)
}
