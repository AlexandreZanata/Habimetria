package service

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"strings"
	"testing"
	"time"

	"vault/internal/store/postgres"
)

type fakeReportStore struct {
	rep       postgres.Report
	err       error
	attachErr error
	attached  int
}

func (f *fakeReportStore) Get(context.Context, string) (postgres.Report, error) {
	return f.rep, f.err
}

func (f *fakeReportStore) GetByCode(context.Context, string) (postgres.Report, error) {
	return f.rep, f.err
}

func (f *fakeReportStore) Attach(_ context.Context, _, sha, _ string) (postgres.Report, error) {
	if f.attachErr != nil {
		return postgres.Report{}, f.attachErr
	}
	f.attached++
	rep := f.rep
	rep.ReportSHA256 = &sha
	rep.PDFReady = true
	return rep, nil
}

var testPDF = []byte("%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF")

func TestAttachHappy(t *testing.T) {
	st := &fakeStorage{}
	fs := &fakeReportStore{rep: postgres.Report{ID: "r1", VerifyCode: "ABC"}}
	svc := NewReportWithStore(nil, fs, st, "https://example.com")
	out, err := svc.Attach(context.Background(), AttachInput{
		ReportID: "123e4567-e89b-12d3-a456-426614174000",
		Mime:     "application/pdf", Content: bytes.NewReader(testPDF),
	})
	if err != nil {
		t.Fatalf("attach: %v", err)
	}
	if out.ReportSHA256 == nil || !out.PDFReady || fs.attached != 1 {
		t.Fatalf("bad result: %+v", out)
	}
	if len(st.putKeys) != 1 || !strings.HasPrefix(st.putKeys[0], "reports/") {
		t.Fatalf("bad storage keys: %v", st.putKeys)
	}
}

func TestAttachRejectsNonPDF(t *testing.T) {
	svc := NewReportWithStore(nil, &fakeReportStore{}, &fakeStorage{}, "https://example.com")
	_, err := svc.Attach(context.Background(), AttachInput{
		ReportID: "123e4567-e89b-12d3-a456-426614174000",
		Content:  bytes.NewReader([]byte("not a pdf at all....")),
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}

func TestAttachTooLarge(t *testing.T) {
	old := MaxPDFBytes
	MaxPDFBytes = 10
	defer func() { MaxPDFBytes = old }()
	st := &fakeStorage{}
	svc := NewReportWithStore(nil, &fakeReportStore{}, st, "https://example.com")
	_, err := svc.Attach(context.Background(), AttachInput{
		ReportID: "123e4567-e89b-12d3-a456-426614174000",
		Content:  bytes.NewReader(testPDF),
	})
	if !errors.Is(err, ErrTooLarge) {
		t.Fatalf("want ErrTooLarge, got %v", err)
	}
	if len(st.putKeys) != 0 {
		t.Fatalf("oversize must not reach storage")
	}
}

func TestAttachDBFailureDeletesOrphan(t *testing.T) {
	st := &fakeStorage{}
	svc := NewReportWithStore(nil, &fakeReportStore{attachErr: errors.New("db down")}, st, "https://example.com")
	_, err := svc.Attach(context.Background(), AttachInput{
		ReportID: "123e4567-e89b-12d3-a456-426614174000",
		Content:  bytes.NewReader(testPDF),
	})
	if err == nil {
		t.Fatal("want error")
	}
	if len(st.deleted) != 1 {
		t.Fatalf("orphan not cleaned: %v", st.deleted)
	}
}

func TestVerifySignature(t *testing.T) {
	const key = "verify-key"
	mac := hmac.New(sha256.New, []byte(key))
	mac.Write([]byte("manifest-sha"))
	sig := hex.EncodeToString(mac.Sum(nil))
	sha := "report-sha"
	svc := NewReportWithStore(nil, &fakeReportStore{rep: postgres.Report{
		VerifyCode: "ABC", ManifestSHA: "manifest-sha", Signature: sig,
		EvidenceCount: 3, ReportSHA256: &sha, PDFReady: true,
		SealedAt: time.Now().UTC(),
	}}, &fakeStorage{}, "https://example.com")
	out, err := svc.Verify(context.Background(), "ABC", key)
	if err != nil {
		t.Fatal(err)
	}
	if !out.Authentic || !out.SignatureValid || out.EvidenceCount != 3 || !out.PDFReady {
		t.Fatalf("bad verify: %+v", out)
	}
	bad, err := svc.Verify(context.Background(), "ABC", "wrong-key")
	if err != nil {
		t.Fatal(err)
	}
	if !bad.Authentic || bad.SignatureValid {
		t.Fatalf("wrong key must flag signature: %+v", bad)
	}
}

func TestVerifyUnknownCode(t *testing.T) {
	svc := NewReportWithStore(nil, &fakeReportStore{err: postgres.ErrNotFound}, &fakeStorage{}, "https://example.com")
	_, err := svc.Verify(context.Background(), "NOPE", "k")
	if !errors.Is(err, postgres.ErrNotFound) {
		t.Fatalf("want ErrNotFound, got %v", err)
	}
}
