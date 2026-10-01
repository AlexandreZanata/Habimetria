package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// CompareTags is the closed visual-state vocabulary. Tags never assign
// liability; the user confirms every tag on device.
var CompareTags = []string{"no_change", "changed", "wear"}

// ShotCategories mirrors the guided capture positions.
var ShotCategories = []string{"overview", "walls", "openings", "floor", "ceiling"}

// CompareService syncs user-confirmed entry-vs-exit tags. Retags
// overwrite the server row; tag history stays on device.
type CompareService struct {
	pool *pgxpool.Pool
}

// NewCompare returns a CompareService bound to the pool.
func NewCompare(pool *pgxpool.Pool) *CompareService {
	return &CompareService{pool: pool}
}

// Upsert validates and records one tag per exit position. Unknown rows
// map to ErrNotFound (404); cross-tenancy or wrong-kind rooms to
// ErrMismatch (400).
func (s *CompareService) Upsert(
	ctx context.Context,
	tenancyID, entryRoomID, exitRoomID, shotCategory string,
	entryEvidenceID, exitEvidenceID *string,
	tag string,
) (postgres.Comparison, error) {
	if err := checkUUID(tenancyID, "tenancy_id"); err != nil {
		return postgres.Comparison{}, err
	}
	if err := checkUUID(entryRoomID, "entry_room_id"); err != nil {
		return postgres.Comparison{}, err
	}
	if err := checkUUID(exitRoomID, "exit_room_id"); err != nil {
		return postgres.Comparison{}, err
	}
	shotCategory = strings.ToLower(strings.TrimSpace(shotCategory))
	if !validValue(shotCategory, ShotCategories) {
		return postgres.Comparison{}, fmt.Errorf(
			"shot_category must be overview|walls|openings|floor|ceiling: %w", ErrInvalid,
		)
	}
	tag = strings.ToLower(strings.TrimSpace(tag))
	if !validValue(tag, CompareTags) {
		return postgres.Comparison{}, fmt.Errorf(
			"tag must be no_change|changed|wear: %w", ErrInvalid,
		)
	}
	entryEv, err := cleanOptionalID(entryEvidenceID, "entry_evidence_id")
	if err != nil {
		return postgres.Comparison{}, err
	}
	exitEv, err := cleanOptionalID(exitEvidenceID, "exit_evidence_id")
	if err != nil {
		return postgres.Comparison{}, err
	}
	return postgres.UpsertComparison(ctx, s.pool, postgres.ComparisonInput{
		TenancyID:       tenancyID,
		EntryRoomID:     entryRoomID,
		ExitRoomID:      exitRoomID,
		ShotCategory:    shotCategory,
		EntryEvidenceID: entryEv,
		ExitEvidenceID:  exitEv,
		Tag:             tag,
	})
}

// List returns a tenancy's tags, oldest first. Unknown tenancy maps to
// ErrNotFound (404).
func (s *CompareService) List(ctx context.Context, tenancyID string) ([]postgres.Comparison, error) {
	if err := checkUUID(tenancyID, "tenancy_id"); err != nil {
		return nil, err
	}
	exists, err := postgres.TenancyExists(ctx, s.pool, tenancyID)
	if err != nil {
		return nil, err
	}
	if !exists {
		return nil, fmt.Errorf("tenancy %s: %w", tenancyID, postgres.ErrNotFound)
	}
	return postgres.ListComparisons(ctx, s.pool, tenancyID)
}

func validValue(v string, allowed []string) bool {
	for _, a := range allowed {
		if v == a {
			return true
		}
	}
	return false
}

// cleanOptionalID trims an optional id reference. Blank becomes nil;
// malformed non-blank is a validation failure (nil-pool safe).
func cleanOptionalID(raw *string, field string) (*string, error) {
	if raw == nil {
		return nil, nil
	}
	trimmed := strings.TrimSpace(*raw)
	if trimmed == "" {
		return nil, nil
	}
	if err := checkUUID(trimmed, field); err != nil {
		return nil, err
	}
	return &trimmed, nil
}
