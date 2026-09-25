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

type stubShare struct {
	share    postgres.Share
	timeline []postgres.ShareTimeline
	view     service.ShareView
	url      string
	err      error
}

func (s stubShare) Create(context.Context, string, string, string) (postgres.Share, error) {
	return s.share, s.err
}

func (s stubShare) Timeline(context.Context, string) ([]postgres.ShareTimeline, error) {
	return s.timeline, s.err
}

func (s stubShare) Open(context.Context, string, string) (service.ShareView, error) {
	return s.view, s.err
}

func (s stubShare) DownloadURL(context.Context, string, string) (string, error) {
	return s.url, s.err
}

func (s stubShare) ShareURL(token string) string {
	return "https://example.com/s/" + token
}

func TestShareCreateBadChannel(t *testing.T) {
	h := NewShareHandler(stubShare{err: service.ErrInvalid})
	req := httptest.NewRequest("POST", "/api/v1/reports/x/shares", strings.NewReader(`{"channel":"pigeon"}`))
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Create(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestShareOpenNotFound(t *testing.T) {
	h := NewShareHandler(stubShare{err: postgres.ErrNotFound})
	req := httptest.NewRequest("GET", "/s/NOPE", nil)
	req.SetPathValue("token", "NOPE")
	rec := httptest.NewRecorder()
	h.Open(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

func TestShareDownloadRedirects(t *testing.T) {
	h := NewShareHandler(stubShare{url: "https://cdn.example/signed"})
	req := httptest.NewRequest("GET", "/s/ABC/pdf", nil)
	req.SetPathValue("token", "ABC")
	rec := httptest.NewRecorder()
	h.DownloadPDF(rec, req)
	if rec.Code != http.StatusFound {
		t.Fatalf("want 302, got %d", rec.Code)
	}
	if loc := rec.Header().Get("Location"); loc != "https://cdn.example/signed" {
		t.Fatalf("bad location: %s", loc)
	}
}
