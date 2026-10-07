package httpapi

import (
	"bytes"
	"context"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"strings"
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

// The public verify view is an allowlisted shape: any new key fails this
// test until the redaction policy reviews it, and forbidden personal-data
// keys (identity, contact, location) must never appear here.
func TestVerifyPublicKeyAllowlist(t *testing.T) {
	sha := "ab"
	h := NewVerifyHandler(stubReport{verify: service.VerifyResult{
		Authentic: true, VerifyCode: "ABC", SealedAt: "t", EvidenceCount: 1,
		ManifestSHA256: "m", SignatureValid: true, ReportSHA256: &sha, PDFReady: true,
	}}, "k")
	req := httptest.NewRequest("GET", "/v/ABC", nil)
	req.SetPathValue("code", "ABC")
	rec := httptest.NewRecorder()
	h.Verify(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", rec.Code)
	}
	var body map[string]any
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("decode public view: %v", err)
	}
	allowed := map[string]bool{
		"authentic": true, "verify_code": true, "sealed_at": true,
		"evidence_count": true, "manifest_sha256": true, "signature_valid": true,
		"report_sha256": true, "pdf_ready": true,
	}
	for key := range body {
		if !allowed[key] {
			t.Fatalf("unreviewed public key %q: update the redaction policy first", key)
		}
		lower := strings.ToLower(key)
		for _, forbidden := range []string{"cpf", "email", "phone", "contact", "address", "gps", "lat", "name"} {
			if strings.Contains(lower, forbidden) {
				t.Fatalf("personal-data key %q in public view", key)
			}
		}
	}
}
