package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Report is the sealed vault's public face: manifest anchor plus the
// final app-rendered PDF artifact (attached in the S7 two-pass upload).
type Report struct {
	ID            string     `json:"report_id"`
	InspectionID  string     `json:"inspection_id"`
	VerifyCode    string     `json:"verify_code"`
	ReportSHA256  *string    `json:"report_sha256"`
	PDFReady      bool       `json:"pdf_ready"`
	SealedAt      time.Time  `json:"sealed_at"`
	EvidenceCount int        `json:"evidence_count"`
	ManifestSHA   string     `json:"manifest_sha256"`
	Signature     string     `json:"signature"`
	PDFKey        string     `json:"-"`
}

const reportSelect = `SELECT r.id::text, r.inspection_id::text, r.verify_code,
       r.report_sha256, r.pdf_key, i.sealed_at, m.sha256, m.signature,
       (SELECT COUNT(*) FROM evidences e WHERE e.inspection_id = i.id)
 FROM reports r
 JOIN inspections i ON i.id = r.inspection_id
 JOIN manifests m ON m.inspection_id = i.id`

func scanReport(row pgx.Row) (Report, error) {
	var rep Report
	var pdfKey *string
	err := row.Scan(&rep.ID, &rep.InspectionID, &rep.VerifyCode,
		&rep.ReportSHA256, &pdfKey, &rep.SealedAt, &rep.ManifestSHA,
		&rep.Signature, &rep.EvidenceCount)
	if err != nil {
		return Report{}, err
	}
	if pdfKey != nil {
		rep.PDFKey = *pdfKey
		rep.PDFReady = true
	}
	return rep, nil
}

// GetReport returns one report with its seal anchors.
func GetReport(ctx context.Context, pool *pgxpool.Pool, reportID string) (Report, error) {
	rep, err := scanReport(pool.QueryRow(ctx, reportSelect+` WHERE r.id=$1`, reportID))
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Report{}, fmt.Errorf("report %s: %w", reportID, ErrNotFound)
		}
		return Report{}, fmt.Errorf("get report: %w", err)
	}
	return rep, nil
}

// GetReportByCode resolves a public verification code.
func GetReportByCode(ctx context.Context, pool *pgxpool.Pool, code string) (Report, error) {
	rep, err := scanReport(pool.QueryRow(ctx, reportSelect+` WHERE r.verify_code=$1`, code))
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Report{}, fmt.Errorf("code %s: %w", code, ErrNotFound)
		}
		return Report{}, fmt.Errorf("get report by code: %w", err)
	}
	return rep, nil
}

// AttachPDF stores the final app-rendered PDF hash. Re-upload overwrites:
// the manifest (trust anchor) never changes, the PDF is a rendering.
// Requires a SEALED inspection.
func AttachPDF(ctx context.Context, pool *pgxpool.Pool, reportID, sha, key string) (Report, error) {
	var inspectionID, status string
	err := pool.QueryRow(ctx,
		`SELECT r.inspection_id::text, i.status FROM reports r
		 JOIN inspections i ON i.id = r.inspection_id WHERE r.id=$1`,
		reportID,
	).Scan(&inspectionID, &status)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Report{}, fmt.Errorf("report %s: %w", reportID, ErrNotFound)
		}
		return Report{}, fmt.Errorf("check report: %w", err)
	}
	if status != "SEALED" {
		return Report{}, fmt.Errorf("inspection %s: %w", inspectionID, ErrSealed)
	}
	if _, err := pool.Exec(ctx,
		`UPDATE reports SET report_sha256=$2, pdf_key=$3 WHERE id=$1`,
		reportID, sha, key,
	); err != nil {
		return Report{}, fmt.Errorf("attach pdf: %w", err)
	}
	return GetReport(ctx, pool, reportID)
}
