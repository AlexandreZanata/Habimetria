package service

import (
	"context"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// RequiredShotsPerRoom is the v0 coverage heuristic: 4 guided photos
// (overview, walls, openings, floor) make a room 100%. The app refines
// this with its shot checklist; the server exposes honest counts.
const RequiredShotsPerRoom = 4

// RoomCoverage is one room's coverage view.
type RoomCoverage struct {
	RoomID   string `json:"room_id"`
	Key      string `json:"key"`
	Label    string `json:"label"`
	Photos   int    `json:"photos"`
	Required int    `json:"required"`
	Percent  int    `json:"percent"`
}

// Coverage is the inspection coverage rollup.
type Coverage struct {
	InspectionID string         `json:"inspection_id"`
	TotalPercent int            `json:"total_percent"`
	Rooms        []RoomCoverage `json:"rooms"`
}

// CoverageService computes the no-AI coverage index.
type CoverageService struct {
	pool *pgxpool.Pool
}

// NewCoverage returns a CoverageService bound to the pool.
func NewCoverage(pool *pgxpool.Pool) *CoverageService {
	return &CoverageService{pool: pool}
}

// Get returns per-room and total coverage for an inspection.
func (s *CoverageService) Get(ctx context.Context, inspectionID string) (Coverage, error) {
	if err := checkUUID(inspectionID, "inspection_id"); err != nil {
		return Coverage{}, err
	}
	rows, err := postgres.GetCoverage(ctx, s.pool, inspectionID)
	if err != nil {
		return Coverage{}, err
	}
	out := Coverage{InspectionID: inspectionID, Rooms: []RoomCoverage{}}
	sum := 0
	for _, r := range rows {
		p := RoomPercent(r.Photos)
		out.Rooms = append(out.Rooms, RoomCoverage{
			RoomID: r.RoomID, Key: r.Key, Label: r.Label,
			Photos: r.Photos, Required: RequiredShotsPerRoom, Percent: p,
		})
		sum += p
	}
	if len(out.Rooms) > 0 {
		out.TotalPercent = sum / len(out.Rooms)
	}
	return out, nil
}

// RoomPercent maps photo count to 0–100, capped at required shots.
func RoomPercent(photos int) int {
	p := photos * (100 / RequiredShotsPerRoom)
	if p > 100 {
		return 100
	}
	if p < 0 {
		return 0
	}
	return p
}
