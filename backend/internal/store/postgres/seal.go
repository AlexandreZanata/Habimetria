package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// SealEvidence is one manifest line, ordered by sequence.
type SealEvidence struct {
	Sequence         int
	EvidenceID       string
	SHA256           string
	Mime             string
	Size             int64
	RoomID           *string
	ReceivedAtServer time.Time
}

// SealParams carries service-computed seal artifacts into the transaction.
// ExpectedCount guards the upload-during-seal race: the tx re-reads the
// evidence set and aborts with ErrChanged on mismatch.
type SealParams struct {
	ManifestBody  []byte
	ManifestSHA   string
	Signature     string
	SealedAt      time.Time
	ExpectedCount int
}

// SealResult is the seal ack returned to the app for PDF embedding.
type SealResult struct {
	InspectionID   string    `json:"inspection_id"`
	Status         string    `json:"status"`
	SealedAt       time.Time `json:"sealed_at"`
	EvidenceCount  int       `json:"evidence_count"`
	ManifestSHA256 string    `json:"manifest_sha256"`
	Signature      string    `json:"signature"`
	ReportID       string    `json:"report_id"`
	VerifyCode     string    `json:"verify_code"`
}

// Seal atomically: locks the inspection, requires DRAFT + ≥1 evidence,
// stores the manifest, flips to SEALED, creates the report row.
// genCode supplies candidate verify codes; unique conflicts retry.
func Seal(ctx context.Context, pool *pgxpool.Pool, inspectionID string, p SealParams, genCode func() (string, error)) (SealResult, error) {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return SealResult{}, fmt.Errorf("begin: %w", err)
	}
	defer tx.Rollback(ctx)

	var status string
	if err := tx.QueryRow(ctx, `SELECT status FROM inspections WHERE id=$1 FOR UPDATE`, inspectionID).Scan(&status); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return SealResult{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrNotFound)
		}
		return SealResult{}, fmt.Errorf("lock inspection: %w", err)
	}
	if status != "DRAFT" {
		return SealResult{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrSealed)
	}

	rows, err := tx.Query(ctx,
		`SELECT sequence, id::text, sha256, mime, size, room_id::text, received_at_server
		 FROM evidences WHERE inspection_id=$1 ORDER BY sequence`,
		inspectionID,
	)
	if err != nil {
		return SealResult{}, fmt.Errorf("list evidences: %w", err)
	}
	evidences := []SealEvidence{}
	for rows.Next() {
		var se SealEvidence
		if err := rows.Scan(&se.Sequence, &se.EvidenceID, &se.SHA256, &se.Mime, &se.Size, &se.RoomID, &se.ReceivedAtServer); err != nil {
			rows.Close()
			return SealResult{}, fmt.Errorf("scan evidence: %w", err)
		}
		evidences = append(evidences, se)
	}
	rows.Close()
	if err := rows.Err(); err != nil {
		return SealResult{}, fmt.Errorf("iterate evidences: %w", err)
	}
	if len(evidences) == 0 {
		return SealResult{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrEmpty)
	}
	if len(evidences) != p.ExpectedCount {
		return SealResult{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrChanged)
	}

	if _, err := tx.Exec(ctx,
		`INSERT INTO manifests (inspection_id, body, sha256, signature) VALUES ($1,$2,$3,$4)`,
		inspectionID, string(p.ManifestBody), p.ManifestSHA, p.Signature,
	); err != nil {
		return SealResult{}, fmt.Errorf("store manifest: %w", err)
	}

	var reportID string
	for attempt := 0; attempt < 5; attempt++ {
		code, err := genCode()
		if err != nil {
			return SealResult{}, fmt.Errorf("verify code: %w", err)
		}
		err = tx.QueryRow(ctx,
			`INSERT INTO reports (inspection_id, verify_code) VALUES ($1,$2) RETURNING id::text`,
			inspectionID, code,
		).Scan(&reportID)
		if err == nil {
			return commitSeal(ctx, tx, inspectionID, p, SealResult{
				InspectionID: inspectionID, Status: "SEALED", SealedAt: p.SealedAt,
				EvidenceCount: len(evidences), ManifestSHA256: p.ManifestSHA,
				Signature: p.Signature, ReportID: reportID, VerifyCode: code,
			})
		}
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23505" {
			continue // verify_code collision, retry with a fresh code
		}
		return SealResult{}, fmt.Errorf("insert report: %w", err)
	}
	return SealResult{}, fmt.Errorf("verify code collisions after retries")
}

// SealEvidences lists manifest lines outside a seal (for manifest build).
// The seal itself re-reads inside the transaction; this is for pre-flight.
func SealEvidences(ctx context.Context, pool *pgxpool.Pool, inspectionID string) ([]SealEvidence, error) {
	rows, err := pool.Query(ctx,
		`SELECT sequence, id::text, sha256, mime, size, room_id::text, received_at_server
		 FROM evidences WHERE inspection_id=$1 ORDER BY sequence`,
		inspectionID,
	)
	if err != nil {
		return nil, fmt.Errorf("list evidences: %w", err)
	}
	defer rows.Close()
	out := []SealEvidence{}
	for rows.Next() {
		var se SealEvidence
		if err := rows.Scan(&se.Sequence, &se.EvidenceID, &se.SHA256, &se.Mime, &se.Size, &se.RoomID, &se.ReceivedAtServer); err != nil {
			return nil, fmt.Errorf("scan evidence: %w", err)
		}
		out = append(out, se)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate evidences: %w", err)
	}
	return out, nil
}

func commitSeal(ctx context.Context, tx pgx.Tx, inspectionID string, p SealParams, res SealResult) (SealResult, error) {
	if _, err := tx.Exec(ctx,
		`UPDATE inspections SET status='SEALED', sealed_at=$2, manifest_sha256=$3, report_id=$4 WHERE id=$1`,
		inspectionID, p.SealedAt, p.ManifestSHA, res.ReportID,
	); err != nil {
		return SealResult{}, fmt.Errorf("seal inspection: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return SealResult{}, fmt.Errorf("commit: %w", err)
	}
	return res, nil
}
