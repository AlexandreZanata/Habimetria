package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ShareService creates share links and serves the public share view.
// Every open/download is logged: created | accessed | downloaded.
type ShareService struct {
	pool          *pgxpool.Pool
	storage       EvidenceStorage
	verifyBaseURL string
	hmacKey       string
}

// NewShare returns a ShareService.
func NewShare(pool *pgxpool.Pool, storage EvidenceStorage, verifyBaseURL, hmacKey string) *ShareService {
	return &ShareService{pool: pool, storage: storage, verifyBaseURL: verifyBaseURL, hmacKey: hmacKey}
}

// ShareURL is the public link handed to WhatsApp/Email/copy.
func (s *ShareService) ShareURL(token string) string {
	return s.verifyBaseURL + "/s/" + token
}

// Create validates the channel, mints a unique token, and logs created.
// Unknown report maps to ErrNotFound (404).
func (s *ShareService) Create(ctx context.Context, reportID, channel, ip string) (postgres.Share, error) {
	if err := checkUUID(reportID, "report_id"); err != nil {
		return postgres.Share{}, err
	}
	channel = strings.ToLower(strings.TrimSpace(channel))
	if channel != "whatsapp" && channel != "email" && channel != "link" {
		return postgres.Share{}, fmt.Errorf("channel must be whatsapp|email|link: %w", ErrInvalid)
	}
	for attempt := 0; attempt < 5; attempt++ {
		token, err := GenerateVerifyCode()
		if err != nil {
			return postgres.Share{}, err
		}
		share, err := postgres.CreateShare(ctx, s.pool, reportID, channel, token)
		if err != nil {
			if errors.Is(err, postgres.ErrTokenTaken) {
				continue
			}
			return postgres.Share{}, err
		}
		if err := postgres.LogAccess(ctx, s.pool, share.ID, "created", hashIP(ip, s.hmacKey)); err != nil {
			return postgres.Share{}, err
		}
		return share, nil
	}
	return postgres.Share{}, fmt.Errorf("share token collisions")
}

// ShareView is the public recipient payload. No raw keys, no photos.
type ShareView struct {
	Share         postgres.Share `json:"share"`
	URL           string         `json:"url"`
	VerifyCode    string         `json:"verify_code"`
	SealedAt      string         `json:"sealed_at"`
	EvidenceCount int            `json:"evidence_count"`
	PDFReady      bool           `json:"pdf_ready"`
	PDFURL        string         `json:"pdf_url,omitempty"`
}

// Open resolves a token, logs accessed, and returns the view with a
// fresh 5-minute PDF URL when the final PDF is attached.
func (s *ShareService) Open(ctx context.Context, token, ip string) (ShareView, error) {
	if token == "" {
		return ShareView{}, fmt.Errorf("token: %w", ErrInvalid)
	}
	share, err := postgres.GetShareByToken(ctx, s.pool, token)
	if err != nil {
		return ShareView{}, err
	}
	rep, err := postgres.GetReport(ctx, s.pool, share.ReportID)
	if err != nil {
		return ShareView{}, err
	}
	if err := postgres.LogAccess(ctx, s.pool, share.ID, "accessed", hashIP(ip, s.hmacKey)); err != nil {
		return ShareView{}, err
	}
	view := ShareView{
		Share: share, URL: s.ShareURL(token), VerifyCode: rep.VerifyCode,
		SealedAt:      rep.SealedAt.UTC().Format(time.RFC3339),
		EvidenceCount: rep.EvidenceCount, PDFReady: rep.PDFReady,
	}
	if rep.PDFReady {
		url, err := s.storage.SignedGetURL(ctx, rep.PDFKey, DownloadTTL)
		if err != nil {
			return ShareView{}, fmt.Errorf("sign url: %w", err)
		}
		view.PDFURL = url
	}
	return view, nil
}

// DownloadURL logs downloaded and returns a 302-ready signed PDF URL.
func (s *ShareService) DownloadURL(ctx context.Context, token, ip string) (string, error) {
	if token == "" {
		return "", fmt.Errorf("token: %w", ErrInvalid)
	}
	share, err := postgres.GetShareByToken(ctx, s.pool, token)
	if err != nil {
		return "", err
	}
	rep, err := postgres.GetReport(ctx, s.pool, share.ReportID)
	if err != nil {
		return "", err
	}
	if !rep.PDFReady {
		return "", fmt.Errorf("pdf not attached: %w", ErrInvalid)
	}
	if err := postgres.LogAccess(ctx, s.pool, share.ID, "downloaded", hashIP(ip, s.hmacKey)); err != nil {
		return "", err
	}
	url, err := s.storage.SignedGetURL(ctx, rep.PDFKey, DownloadTTL)
	if err != nil {
		return "", fmt.Errorf("sign url: %w", err)
	}
	return url, nil
}

// Timeline returns shares of a report with access counts.
func (s *ShareService) Timeline(ctx context.Context, reportID string) ([]postgres.ShareTimeline, error) {
	if err := checkUUID(reportID, "report_id"); err != nil {
		return nil, err
	}
	if _, err := postgres.GetReport(ctx, s.pool, reportID); err != nil {
		return nil, err
	}
	return postgres.ListShares(ctx, s.pool, reportID)
}

// hashIP pseudonymizes viewer IPs in the access log.
func hashIP(ip, key string) string {
	mac := hmac.New(sha256.New, []byte(key))
	mac.Write([]byte(ip))
	return hex.EncodeToString(mac.Sum(nil))[:32]
}
