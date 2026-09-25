package httpapi

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type stubFinding struct {
	finding postgres.Finding
	list    []postgres.Finding
	err     error
}

func (s stubFinding) Create(context.Context, string, string, string, *string) (postgres.Finding, error) {
	return s.finding, s.err
}

func (s stubFinding) List(context.Context, string) ([]postgres.Finding, error) {
	return s.list, s.err
}

func TestFindingBadPresetMaps400(t *testing.T) {
	h := NewFindingHandler(stubFinding{err: postgres.ErrMismatch})
	req := httptest.NewRequest("POST", "/api/v1/rooms/x/findings", strings.NewReader(`{"evidence_id":"y","preset":"crack"}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestFindingSealedMaps409(t *testing.T) {
	h := NewFindingHandler(stubFinding{err: postgres.ErrSealed})
	req := httptest.NewRequest("POST", "/api/v1/rooms/x/findings", strings.NewReader(`{"evidence_id":"y","preset":"crack"}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rec.Code)
	}
}

func TestFindingRoomMissingMaps404(t *testing.T) {
	h := NewFindingHandler(stubFinding{err: postgres.ErrNotFound})
	req := httptest.NewRequest("GET", "/api/v1/rooms/x/findings", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.List(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

type stubCoverage struct {
	cov service.Coverage
	err error
}

func (s stubCoverage) Get(context.Context, string) (service.Coverage, error) {
	return s.cov, s.err
}

func TestCoverageBadIDMaps400(t *testing.T) {
	h := NewCoverageHandler(stubCoverage{err: service.ErrInvalid})
	req := httptest.NewRequest("GET", "/api/v1/inspections/nope/coverage", nil)
	req.SetPathValue("id", "nope")
	rec := httptest.NewRecorder()
	h.Get(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}
