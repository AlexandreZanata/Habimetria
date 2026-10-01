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

type stubOccurrence struct {
	occ  postgres.Occurrence
	list []postgres.Occurrence
	err  error
}

func (s stubOccurrence) Create(context.Context, string, string, *string) (postgres.Occurrence, error) {
	return s.occ, s.err
}

func (s stubOccurrence) List(context.Context, string) ([]postgres.Occurrence, error) {
	return s.list, s.err
}

func occurrenceReq(method, target, body string) (*httptest.ResponseRecorder, *http.Request) {
	var reader *strings.Reader
	if body == "" {
		reader = strings.NewReader("")
	} else {
		reader = strings.NewReader(body)
	}
	req := httptest.NewRequest(method, target, reader)
	req.SetPathValue("id", "123e4567-e89b-12d3-a456-426614174000")
	return httptest.NewRecorder(), req
}

func TestOccurrenceCreatePersisted(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{occ: postgres.Occurrence{ID: "o1", Title: "Leak"}})
	rec, req := occurrenceReq("POST", "/api/v1/properties/x/occurrences", `{"title":"Leak"}`)
	h.Create(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d", rec.Code)
	}
}

func TestOccurrenceCreateBadJSON(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{})
	rec, req := occurrenceReq("POST", "/api/v1/properties/x/occurrences", "{bad")
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestOccurrenceCreateValidationMaps400(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{err: service.ErrInvalid})
	rec, req := occurrenceReq("POST", "/api/v1/properties/x/occurrences", `{"title":""}`)
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestOccurrenceMissingPropertyMaps404(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{err: postgres.ErrNotFound})
	rec, req := occurrenceReq("POST", "/api/v1/properties/x/occurrences", `{"title":"Leak"}`)
	h.Create(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
	rec, req = occurrenceReq("GET", "/api/v1/properties/x/occurrences", "")
	h.List(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("list: want 404, got %d", rec.Code)
	}
}

func TestOccurrenceListOK(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{list: []postgres.Occurrence{}})
	rec, req := occurrenceReq("GET", "/api/v1/properties/x/occurrences", "")
	h.List(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
}
