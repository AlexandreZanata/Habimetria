package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// MaxNoteLength caps the optional finding note.
const MaxNoteLength = 500

// FindingPresets is the closed defect vocabulary. No free text required.
var FindingPresets = []string{"stain", "scratch", "crack", "broken", "moisture", "hole", "other"}

// FindingService records defects: photo evidence + preset + optional note.
type FindingService struct {
	pool *pgxpool.Pool
}

// NewFinding returns a FindingService bound to the pool.
func NewFinding(pool *pgxpool.Pool) *FindingService {
	return &FindingService{pool: pool}
}

// Create validates ids and preset, then inserts the finding.
// The room's inspection must be DRAFT; evidence must belong to it.
func (s *FindingService) Create(ctx context.Context, roomID, evidenceID, preset string, note *string) (postgres.Finding, error) {
	if err := checkUUID(roomID, "room_id"); err != nil {
		return postgres.Finding{}, err
	}
	if err := checkUUID(evidenceID, "evidence_id"); err != nil {
		return postgres.Finding{}, err
	}
	preset = strings.ToLower(strings.TrimSpace(preset))
	if !validPreset(preset) {
		return postgres.Finding{}, fmt.Errorf("preset must be stain|scratch|crack|broken|moisture|hole|other: %w", ErrInvalid)
	}
	if note != nil {
		trimmed := strings.TrimSpace(*note)
		note = &trimmed
		if len(trimmed) > MaxNoteLength {
			return postgres.Finding{}, fmt.Errorf("note exceeds %d chars: %w", MaxNoteLength, ErrInvalid)
		}
		if trimmed == "" {
			note = nil
		}
	}
	return postgres.CreateFinding(ctx, s.pool, roomID, evidenceID, preset, note)
}

// List returns findings of one room, newest last.
func (s *FindingService) List(ctx context.Context, roomID string) ([]postgres.Finding, error) {
	if err := checkUUID(roomID, "room_id"); err != nil {
		return nil, err
	}
	ok, err := postgres.RoomExists(ctx, s.pool, roomID)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fmt.Errorf("room %s: %w", roomID, postgres.ErrNotFound)
	}
	return postgres.ListFindings(ctx, s.pool, roomID)
}

func validPreset(p string) bool {
	for _, v := range FindingPresets {
		if p == v {
			return true
		}
	}
	return false
}
