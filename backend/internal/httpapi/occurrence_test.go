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
	occ       postgres.Occurrence
	list      []postgres.Occurrence
	link      postgres.OccurrenceEvidence
	evidences []postgres.Evidence
	err       error
}

func (s stubOccurrence) Create(context.Context, string, string, *string) (postgres.Occurrence, error) {
	return s.occ, s.err
}

func (s stubOccurrence) List(context.Context, string) ([]postgres.Occurrence, error) {
	return s.list, s.err
}

func (s stubOccurrence) AttachEvidence(context.Context, string, string) (postgres.OccurrenceEvidence, error) {
	return s.link, s.err
}

func (s stubOccurrence) ListEvidences(context.Context, string) ([]postgres.Evidence, error) {
	return s.evidences, s.err
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

func TestOccurrenceAttachPersisted(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{})
	rec, req := occurrenceReq("POST", "/api/v1/occurrences/x/evidences", `{"evidence_id":"123e4567-e89b-12d3-a456-426614174001"}`)
	h.Attach(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d", rec.Code)
	}
}

func TestOccurrenceAttachBadJSON(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{})
	rec, req := occurrenceReq("POST", "/api/v1/occurrences/x/evidences", "{bad")
	h.Attach(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestOccurrenceAttachMapsStatuses(t *testing.T) {
	for _, tc := range []struct {
		err  error
		want int
	}{
		{service.ErrInvalid, http.StatusBadRequest},
		{postgres.ErrNotFound, http.StatusNotFound},
		{postgres.ErrMismatch, http.StatusBadRequest},
		{postgres.ErrAlreadyLinked, http.StatusConflict},
	} {
		h := NewOccurrenceHandler(stubOccurrence{err: tc.err})
		rec, req := occurrenceReq("POST", "/api/v1/occurrences/x/evidences", `{"evidence_id":"x"}`)
		h.Attach(rec, req)
		if rec.Code != tc.want {
			t.Fatalf("err %v: want %d, got %d", tc.err, tc.want, rec.Code)
		}
	}
}

func TestOccurrenceListEvidences(t *testing.T) {
	h := NewOccurrenceHandler(stubOccurrence{evidences: []postgres.Evidence{}})
	rec, req := occurrenceReq("GET", "/api/v1/occurrences/x/evidences", "")
	h.ListEvidences(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
	h = NewOccurrenceHandler(stubOccurrence{err: postgres.ErrNotFound})
	rec, req = occurrenceReq("GET", "/api/v1/occurrences/x/evidences", "")
	h.ListEvidences(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}
