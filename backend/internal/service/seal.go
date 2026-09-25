package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// SealInput carries the seal request. PaymentProof is accepted but only
// checked by the provider; the stub approves everything until S6.
type SealInput struct {
	InspectionID string
	PaymentProof string
}

// SealOutput is the seal ack. The app embeds manifest_sha256,
// verify_code and the verify URL (as QR) into the final PDF.
type SealOutput = postgres.SealResult

// SealService freezes DRAFT inspections into sealed evidence vaults.
type SealService struct {
	pool     *pgxpool.Pool
	payments PaymentPort
	hmacKey  string
}

// NewSeal returns a SealService. hmacKey signs manifests; rotate via env.
func NewSeal(pool *pgxpool.Pool, payments PaymentPort, hmacKey string) *SealService {
	return &SealService{pool: pool, payments: payments, hmacKey: hmacKey}
}

// Seal verifies payment, builds the manifest, signs it, and flips the
// inspection to SEALED atomically. Terminal: no SEALED -> DRAFT.
func (s *SealService) Seal(ctx context.Context, in SealInput) (SealOutput, error) {
	if err := checkUUID(in.InspectionID, "inspection_id"); err != nil {
		return SealOutput{}, err
	}
	if err := s.payments.Verify(ctx, in.PaymentProof); err != nil {
		return SealOutput{}, fmt.Errorf("payment: %w", err)
	}
	for attempt := 0; attempt < 3; attempt++ {
		evidences, err := postgres.SealEvidences(ctx, s.pool, in.InspectionID)
		if err != nil {
			return SealOutput{}, err
		}
		// No empty check here: the tx distinguishes missing (404)
		// from empty (422) atomically under the inspection lock.
		sealedAt := time.Now().UTC()
		body, err := BuildManifest(in.InspectionID, sealedAt, evidences)
		if err != nil {
			return SealOutput{}, err
		}
		sha := sha256.Sum256(body)
		shaHex := hex.EncodeToString(sha[:])
		out, err := postgres.Seal(ctx, s.pool, in.InspectionID, postgres.SealParams{
			ManifestBody: body, ManifestSHA: shaHex,
			Signature: s.sign(shaHex), SealedAt: sealedAt,
			ExpectedCount: len(evidences),
		}, GenerateVerifyCode)
		if err != nil {
			if errors.Is(err, postgres.ErrChanged) {
				continue // upload landed mid-seal; rebuild and retry
			}
			return SealOutput{}, err
		}
		return out, nil
	}
	return SealOutput{}, fmt.Errorf("inspection %s: %w", in.InspectionID, postgres.ErrChanged)
}

func (s *SealService) sign(manifestSHA string) string {
	mac := hmac.New(sha256.New, []byte(s.hmacKey))
	mac.Write([]byte(manifestSHA))
	return hex.EncodeToString(mac.Sum(nil))
}
