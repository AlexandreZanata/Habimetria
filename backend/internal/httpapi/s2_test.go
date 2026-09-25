package httpapi

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type stubProperty struct {
	prop postgres.Property
	err  error
}

func (s stubProperty) Create(context.Context, string) (postgres.Property, error) {
	return s.prop, s.err
}

func TestPropertyCreateBadJSON(t *testing.T) {
	h := NewPropertyHandler(stubProperty{})
	req := httptest.NewRequest("POST", "/api/v1/properties", strings.NewReader("{bad"))
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestPropertyCreateValidationMaps400(t *testing.T) {
	h := NewPropertyHandler(stubProperty{err: service.ErrInvalid})
	req := httptest.NewRequest("POST", "/api/v1/properties", strings.NewReader(`{"address":"x"}`))
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

type stubTenancy struct {
	ten postgres.Tenancy
	err error
}

func (s stubTenancy) Create(context.Context, string) (postgres.Tenancy, error) {
	return s.ten, s.err
}

func TestTenancyNotFoundMaps404(t *testing.T) {
	h := NewTenancyHandler(stubTenancy{err: postgres.ErrNotFound})
	req := httptest.NewRequest("POST", "/api/v1/properties/123e4567-e89b-12d3-a456-426614174000/tenancies", nil)
	req.SetPathValue("id", "123e4567-e89b-12d3-a456-426614174000")
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

type stubRoom struct {
	room  postgres.Room
	rooms []postgres.Room
	err   error
}

func (s stubRoom) Create(context.Context, string, string, string) (postgres.Room, error) {
	return s.room, s.err
}

func (s stubRoom) List(context.Context, string) ([]postgres.Room, error) {
	return s.rooms, s.err
}

func (s stubRoom) EnsureTemplate(context.Context, string) ([]postgres.Room, error) {
	return s.rooms, s.err
}

func TestRoomDuplicateMaps409(t *testing.T) {
	h := NewRoomHandler(stubRoom{err: postgres.ErrRoomExists})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x/rooms", strings.NewReader(`{"key":"kitchen"}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rec.Code)
	}
}

func TestRoomListInternalMaps500(t *testing.T) {
	h := NewRoomHandler(stubRoom{err: errors.New("boom")})
	req := httptest.NewRequest("GET", "/api/v1/inspections/x/rooms", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.List(rec, req)
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("want 500, got %d", rec.Code)
	}
}
