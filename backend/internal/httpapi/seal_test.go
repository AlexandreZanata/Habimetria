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

type stubSeal struct {
	out service.SealOutput
	err error
}

func (s stubSeal) Seal(context.Context, service.SealInput) (service.SealOutput, error) {
	return s.out, s.err
}

func TestSealCreated(t *testing.T) {
	h := NewSealHandler(stubSeal{out: service.SealOutput{Status: "SEALED", VerifyCode: "ABC123"}})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x:seal", strings.NewReader(`{}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Seal(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d", rec.Code)
	}
}

func TestSealAlreadySealedMaps409(t *testing.T) {
	h := NewSealHandler(stubSeal{err: postgres.ErrSealed})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x:seal", strings.NewReader(`{}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Seal(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rec.Code)
	}
}

func TestSealEmptyMaps422(t *testing.T) {
	h := NewSealHandler(stubSeal{err: postgres.ErrEmpty})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x:seal", strings.NewReader(`{}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Seal(rec, req)
	if rec.Code != http.StatusUnprocessableEntity {
		t.Fatalf("want 422, got %d", rec.Code)
	}
}

func TestSealMissingMaps404(t *testing.T) {	h := NewSealHandler(stubSeal{err: postgres.ErrNotFound})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x:seal", strings.NewReader(`{}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Seal(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

func TestSealUnpaidMaps402(t *testing.T) {
	h := NewSealHandler(stubSeal{err: service.ErrPayment})
	req := httptest.NewRequest("POST", "/api/v1/inspections/x/seal", strings.NewReader(`{}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Seal(rec, req)
	if rec.Code != http.StatusPaymentRequired {
		t.Fatalf("want 402, got %d", rec.Code)
	}
}
