package httpapi

import (
	"bytes"
	"context"
	"errors"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type stubEvidence struct {
	result service.UploadResult
	upErr  error
	url    string
	urlErr error
	lines  []postgres.EvidenceLine
	listErr error
}

func (s stubEvidence) Upload(context.Context, service.UploadInput) (service.UploadResult, error) {
	return s.result, s.upErr
}

func (s stubEvidence) DownloadURL(context.Context, string, string) (string, time.Time, error) {
	return s.url, time.Now(), s.urlErr
}

func (s stubEvidence) List(context.Context, string) ([]postgres.EvidenceLine, error) {
	return s.lines, s.listErr
}

func multipartBody(t *testing.T, withFile bool) (*bytes.Buffer, string) {
	t.Helper()
	var buf bytes.Buffer
	w := multipart.NewWriter(&buf)
	if err := w.WriteField("type", "photo"); err != nil {
		t.Fatal(err)
	}
	if withFile {
		fw, err := w.CreateFormFile("file", "wall.jpg")
		if err != nil {
			t.Fatal(err)
		}
		if _, err := fw.Write([]byte("bytes")); err != nil {
			t.Fatal(err)
		}
	}
	if err := w.Close(); err != nil {
		t.Fatal(err)
	}
	return &buf, w.FormDataContentType()
}

func TestEvidenceUploadMissingFile(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{})
	buf, ctype := multipartBody(t, false)
	req := httptest.NewRequest("POST", "/api/v1/inspections/x/evidences", buf)
	req.Header.Set("Content-Type", ctype)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Upload(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestEvidenceUploadSealedMaps409(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{upErr: postgres.ErrSealed})
	buf, ctype := multipartBody(t, true)
	req := httptest.NewRequest("POST", "/api/v1/inspections/x/evidences", buf)
	req.Header.Set("Content-Type", ctype)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Upload(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rec.Code)
	}
}

func TestEvidenceUploadTooLargeMaps413(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{upErr: service.ErrTooLarge})
	buf, ctype := multipartBody(t, true)
	req := httptest.NewRequest("POST", "/api/v1/inspections/x/evidences", buf)
	req.Header.Set("Content-Type", ctype)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.Upload(rec, req)
	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("want 413, got %d", rec.Code)
	}
}

func TestEvidenceDownloadMissingParam(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{})
	req := httptest.NewRequest("GET", "/api/v1/inspections/x/evidences/download-url", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.DownloadURL(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestEvidenceDownloadNotFound(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{urlErr: postgres.ErrNotFound})
	req := httptest.NewRequest("GET", "/api/v1/inspections/x/evidences/download-url?evidence_id=y", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.DownloadURL(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d: %v", rec.Code, errors.New(rec.Body.String()))
	}
}

func TestEvidenceListNotFound(t *testing.T) {
	h := NewEvidenceHandler(stubEvidence{listErr: postgres.ErrNotFound})
	req := httptest.NewRequest("GET", "/api/v1/inspections/x/evidences", nil)
	req.SetPathValue("id", "x")
	rec := httptest.NewRecorder()
	h.List(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rec.Code)
	}
}
