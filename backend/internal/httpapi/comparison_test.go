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

type stubCompare struct {
	cmp  postgres.Comparison
	list []postgres.Comparison
	err  error
}

func (s stubCompare) Upsert(
	context.Context, string, string, string, string, *string, *string, string,
) (postgres.Comparison, error) {
	return s.cmp, s.err
}

func (s stubCompare) List(context.Context, string) ([]postgres.Comparison, error) {
	return s.list, s.err
}

func compareReq(body string) (*httptest.ResponseRecorder, *http.Request) {
	req := httptest.NewRequest(
		"POST", "/api/v1/tenancies/x/comparisons", strings.NewReader(body),
	)
	req.SetPathValue("id", "123e4567-e89b-12d3-a456-426614174000")
	return httptest.NewRecorder(), req
}

func TestCompareUpsertOK(t *testing.T) {
	h := NewCompareHandler(stubCompare{cmp: postgres.Comparison{Tag: "changed"}})
	rec, req := compareReq(`{"entry_room_id":"123e4567-e89b-12d3-a456-426614174001",
		"exit_room_id":"123e4567-e89b-12d3-a456-426614174002",
		"shot_category":"walls","tag":"changed"}`)
	h.Upsert(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
}

func TestCompareUpsertBadJSON(t *testing.T) {
	h := NewCompareHandler(stubCompare{})
	rec, req := compareReq("{bad")
	h.Upsert(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestCompareUpsertMapsStatuses(t *testing.T) {
	for _, tc := range []struct {
		err  error
		want int
	}{
		{service.ErrInvalid, http.StatusBadRequest},
		{postgres.ErrNotFound, http.StatusNotFound},
		{postgres.ErrMismatch, http.StatusBadRequest},
	} {
		h := NewCompareHandler(stubCompare{err: tc.err})
		rec, req := compareReq(`{"tag":"changed"}`)
		h.Upsert(rec, req)
		if rec.Code != tc.want {
			t.Fatalf("err %v: want %d, got %d", tc.err, tc.want, rec.Code)
		}
	}
}

func TestCompareList(t *testing.T) {
	h := NewCompareHandler(stubCompare{list: []postgres.Comparison{}})
	req := httptest.NewRequest("GET", "/api/v1/tenancies/x/comparisons", nil)
	req.SetPathValue("id", "123e4567-e89b-12d3-a456-426614174000")
	rec := httptest.NewRecorder()
	h.List(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
	h = NewCompareHandler(stubCompare{err: postgres.ErrNotFound})
	rec = httptest.NewRecorder()
	h.List(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}
