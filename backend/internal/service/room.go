package service

import (
	"context"
	"fmt"
	"strings"

	"vault/internal/store/postgres"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// RoomService manages rooms inside an inspection.
type RoomService struct {
	pool *pgxpool.Pool
}

// NewRoom returns a RoomService bound to the pool.
func NewRoom(pool *pgxpool.Pool) *RoomService {
	return &RoomService{pool: pool}
}

// Create validates and inserts one room. Empty label defaults to key.
func (s *RoomService) Create(ctx context.Context, inspectionID, key, label string) (postgres.Room, error) {
	if _, err := uuid.Parse(inspectionID); err != nil {
		return postgres.Room{}, fmt.Errorf("inspection_id: %w", ErrInvalid)
	}
	key = strings.ToLower(strings.TrimSpace(key))
	if key == "" {
		return postgres.Room{}, fmt.Errorf("key: %w", ErrInvalid)
	}
	label = strings.TrimSpace(label)
	if label == "" {
		label = key
	}
	return postgres.CreateRoom(ctx, s.pool, inspectionID, key, label)
}

// List returns rooms ordered by sort.
func (s *RoomService) List(ctx context.Context, inspectionID string) ([]postgres.Room, error) {
	if _, err := uuid.Parse(inspectionID); err != nil {
		return nil, fmt.Errorf("inspection_id: %w", ErrInvalid)
	}
	return postgres.ListRooms(ctx, s.pool, inspectionID)
}

// EnsureTemplate inserts the default room set, skipping existing keys.
func (s *RoomService) EnsureTemplate(ctx context.Context, inspectionID string) ([]postgres.Room, error) {
	if _, err := uuid.Parse(inspectionID); err != nil {
		return nil, fmt.Errorf("inspection_id: %w", ErrInvalid)
	}
	return postgres.EnsureTemplate(ctx, s.pool, inspectionID)
}
