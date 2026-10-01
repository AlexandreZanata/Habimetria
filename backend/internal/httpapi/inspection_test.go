package httpapi

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/store/postgres"
)

type stubInspection struct {
	in  postgres.Inspection
	err error
}

func (s stubInspection) Create(context.Context, string, string) (postgres.Inspection, error) {
	return s.in, s.err
}

func inspectionReq(body string) (*httptest.ResponseRecorder, *http.Request) {
	req := httptest.NewRequest("POST", "/api/v1/tenancies/x/inspections", strings.NewReader(body))
	req.SetPathValue("id", "123e4567-e89b-12d3-a456-426614174000")
	return httptest.NewRecorder(), req
}

func TestInspectionCreateEntry(t *testing.T) {
	h := NewInspectionHandler(stubInspection{in: postgres.Inspection{ID: "i1", Kind: "entry"}})
	rec, req := inspectionReq(`{"kind":"entry"}`)
	h.Create(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d", rec.Code)
	}
}

func TestInspectionCreateBadJSON(t *testing.T) {
	h := NewInspectionHandler(stubInspection{})
	rec, req := inspectionReq("{bad")
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestInspectionExitNotAllowedMaps409(t *testing.T) {
	h := NewInspectionHandler(stubInspection{err: postgres.ErrExitNotAllowed})
	rec, req := inspectionReq(`{"kind":"exit"}`)
	h.Create(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rec.Code)
	}
}

func TestInspectionMissingTenancyMaps404(t *testing.T) {
	h := NewInspectionHandler(stubInspection{err: postgres.ErrNotFound})
	rec, req := inspectionReq(`{"kind":"exit"}`)
	h.Create(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}
