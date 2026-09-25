package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"time"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// MaxPDFBytes caps the final app-rendered PDF upload. Var for tests.
var MaxPDFBytes = int64(50 << 20) // 50 MB

// PDF magic bytes. The server accepts real PDFs only.
var pdfMagic = []byte("%PDF-")

// ReportService serves report reads, the two-pass PDF attach, and
// public verification.
type ReportService struct {
	pool          *pgxpool.Pool
	store         ReportStore
	storage       EvidenceStorage
	verifyBaseURL string
}

// ReportStore is the SQL surface reports need. Tests use fakes.
type ReportStore interface {
	Get(ctx context.Context, reportID string) (postgres.Report, error)
	GetByCode(ctx context.Context, code string) (postgres.Report, error)
	Attach(ctx context.Context, reportID, sha, key string) (postgres.Report, error)
}

// NewReport returns a ReportService. verifyBaseURL is the public origin
// embedded in QR codes, e.g. https://vault.example.com.
func NewReport(pool *pgxpool.Pool, storage EvidenceStorage, verifyBaseURL string) *ReportService {
	return &ReportService{pool: pool, store: pgReportStore{pool: pool}, storage: storage, verifyBaseURL: verifyBaseURL}
}

// NewReportWithStore injects fakes. Tests only.
func NewReportWithStore(pool *pgxpool.Pool, store ReportStore, storage EvidenceStorage, verifyBaseURL string) *ReportService {
	return &ReportService{pool: pool, store: store, storage: storage, verifyBaseURL: verifyBaseURL}
}

// Detail returns one report plus the QR-ready verify URL.
func (s *ReportService) Detail(ctx context.Context, reportID string) (postgres.Report, string, error) {
	if err := checkUUID(reportID, "report_id"); err != nil {
		return postgres.Report{}, "", err
	}
	rep, err := s.store.Get(ctx, reportID)
	if err != nil {
		return postgres.Report{}, "", err
	}
	return rep, s.verifyBaseURL + "/v/" + rep.VerifyCode, nil
}

// AttachInput is one final PDF upload from the app.
type AttachInput struct {
	ReportID string
	Filename string
	Mime     string
	Content  io.Reader
}

// Attach stores the final PDF: validates magic bytes + size, hashes
// server-side, uploads immutable original, records report_sha256.
func (s *ReportService) Attach(ctx context.Context, in AttachInput) (postgres.Report, error) {
	if err := checkUUID(in.ReportID, "report_id"); err != nil {
		return postgres.Report{}, err
	}
	sha, size, tmp, err := bufferAndHash(in.Content)
	if err != nil {
		return postgres.Report{}, err
	}
	defer os.Remove(tmp.Name())
	defer tmp.Close()
	if size == 0 {
		return postgres.Report{}, fmt.Errorf("empty file: %w", ErrInvalid)
	}
	if size > MaxPDFBytes {
		return postgres.Report{}, fmt.Errorf("pdf exceeds %d bytes: %w", MaxPDFBytes, ErrTooLarge)
	}
	if _, err := tmp.Seek(0, io.SeekStart); err != nil {
		return postgres.Report{}, fmt.Errorf("rewind: %w", err)
	}
	var magic [5]byte
	if _, err := io.ReadFull(tmp, magic[:]); err != nil || string(magic[:]) != string(pdfMagic) {
		return postgres.Report{}, fmt.Errorf("not a PDF: %w", ErrInvalid)
	}
	if _, err := tmp.Seek(0, io.SeekStart); err != nil {
		return postgres.Report{}, fmt.Errorf("rewind: %w", err)
	}
	key := "reports/" + in.ReportID + ".pdf"
	if err := s.storage.PutOriginal(ctx, key, tmp, "application/pdf"); err != nil {
		return postgres.Report{}, err
	}
	rep, err := s.store.Attach(ctx, in.ReportID, sha, key)
	if err != nil {
		_ = s.storage.Delete(ctx, key)
		return postgres.Report{}, err
	}
	return rep, nil
}

// DownloadURL returns a 5-minute signed URL for the final PDF.
func (s *ReportService) DownloadURL(ctx context.Context, reportID string) (string, time.Time, error) {
	if err := checkUUID(reportID, "report_id"); err != nil {
		return "", time.Time{}, err
	}
	rep, err := s.store.Get(ctx, reportID)
	if err != nil {
		return "", time.Time{}, err
	}
	if !rep.PDFReady {
		return "", time.Time{}, fmt.Errorf("pdf not attached: %w", ErrInvalid)
	}
	url, err := s.storage.SignedGetURL(ctx, rep.PDFKey, DownloadTTL)
	if err != nil {
		return "", time.Time{}, fmt.Errorf("sign url: %w", err)
	}
	return url, time.Now().Add(DownloadTTL), nil
}

// VerifyResult is the public verification payload. No photos leak.
type VerifyResult struct {
	Authentic      bool    `json:"authentic"`
	VerifyCode     string  `json:"verify_code"`
	SealedAt       string  `json:"sealed_at"`
	EvidenceCount  int     `json:"evidence_count"`
	ManifestSHA256 string  `json:"manifest_sha256"`
	SignatureValid bool    `json:"signature_valid"`
	ReportSHA256   *string `json:"report_sha256"`
	PDFReady       bool    `json:"pdf_ready"`
}

// Verify resolves a public code and re-checks the HMAC signature.
func (s *ReportService) Verify(ctx context.Context, code string, hmacKey string) (VerifyResult, error) {
	if code == "" {
		return VerifyResult{}, fmt.Errorf("code: %w", ErrInvalid)
	}
	rep, err := s.store.GetByCode(ctx, code)
	if err != nil {
		return VerifyResult{}, err
	}
	mac := hmac.New(sha256.New, []byte(hmacKey))
	mac.Write([]byte(rep.ManifestSHA))
	valid := hmac.Equal(
		[]byte(rep.Signature),
		[]byte(hex.EncodeToString(mac.Sum(nil))),
	)
	return VerifyResult{
		Authentic: true, VerifyCode: rep.VerifyCode,
		SealedAt: rep.SealedAt.UTC().Format(time.RFC3339),
		EvidenceCount: rep.EvidenceCount, ManifestSHA256: rep.ManifestSHA,
		SignatureValid: valid, ReportSHA256: rep.ReportSHA256, PDFReady: rep.PDFReady,
	}, nil
}
