package service

import (
	"context"
	"fmt"
	"time"
)

// Pinger is the minimal DB surface Health needs. *pgxpool.Pool satisfies it.
type Pinger interface {
	Ping(ctx context.Context) error
}

// HealthService owns readiness logic. Handlers never touch the DB.
type HealthService struct {
	db      Pinger
	started time.Time
}

// NewHealth returns a HealthService. db may be nil for boot probe.
func NewHealth(db Pinger) *HealthService {
	return &HealthService{db: db, started: time.Now()}
}

// Status is the JSON shape of GET /healthz and GET /readyz.
type Status struct {
	Status   string `json:"status"`
	Database string `json:"database"`
	Uptime   string `json:"uptime"`
}

// Check pings the DB with a short timeout.
func (s *HealthService) Check(ctx context.Context) Status {
	st := Status{Status: "ok", Database: "not_configured", Uptime: time.Since(s.started).String()}
	if s.db == nil {
		return st
	}
	pingCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	if err := s.db.Ping(pingCtx); err != nil {
		st.Status = "degraded"
		st.Database = fmt.Sprintf("unreachable: %v", err)
		return st
	}
	st.Database = "ok"
	return st
}

// Ready is true only when DB pings cleanly. Used by /readyz.
func (s *HealthService) Ready(ctx context.Context) bool {
	if s.db == nil {
		return false
	}
	pingCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	return s.db.Ping(pingCtx) == nil
}
