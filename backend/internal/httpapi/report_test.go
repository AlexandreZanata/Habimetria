package httpapi

import (
	"bytes"
	"context"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type stubReport struct {
	rep       postgres.Report
	url       string
	detailErr error
	attachErr error
	urlErr    error
	verify    service.VerifyResult
	verifyErr error
}

func (s stubReport) Detail(context.Context, string) (postgres.Report, string, error) {
	return s.rep, s.url, s.detailErr
}

func (s stubReport) Attach(context.Context, service.AttachInput) (postgres.Report, error) {
	return s.rep, s.attachErr
}

func (s stubReport) DownloadURL(context.Context, string) (string, time.Time, error) {
	return s.url, time.Now(), s.urlErr
}

func (s stubReport) Verify(context.Context, string, string) (service.VerifyResult, error) {
	return s.verify, s.verifyErr
}

func TestReportDetailNotFound(t *testing.T) {
	h := NewReportHandler(stubReport{detailErr: postgres.ErrNotFound})
	vh := NewVerifyHandler(stubReport{verifyErr: postgres.ErrNotFound}, "k")
	_ = vh
	req := httptest.NewRequest("GET", "/api/v1/reports/x", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Detail(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

func TestReportAttachNonPDFMaps400(t *testing.T) {
	h := NewReportHandler(stubReport{attachErr: service.ErrInvalid})
	var buf bytes.Buffer
	w := multipart.NewWriter(&buf)
	fw, _ := w.CreateFormFile("file", "evil.txt")
	fw.Write([]byte("hello"))
	w.Close()
	req := httptest.NewRequest("POST", "/api/v1/reports/x/pdf", &buf)
	req.Header.Set("Content-Type", w.FormDataContentType())
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Attach(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestVerifyUnknownMaps404(t *testing.T) {
	h := NewVerifyHandler(stubReport{verifyErr: postgres.ErrNotFound}, "k")
	req := httptest.NewRequest("GET", "/v/NOPE", nil)
	req.SetPathValue("code", "NOPE")
	rec := httptest.NewRecorder()
	h.Verify(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}

func TestVerifyOK(t *testing.T) {
	h := NewVerifyHandler(stubReport{verify: service.VerifyResult{Authentic: true, VerifyCode: "ABC"}}, "k")
	req := httptest.NewRequest("GET", "/v/ABC", nil)
	req.SetPathValue("code", "ABC")
	rec := httptest.NewRecorder()
	h.Verify(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
}
