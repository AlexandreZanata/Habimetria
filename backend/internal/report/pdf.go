package report

// Contract for S7 (PDF worker).
// PDF footer must contain: report ID, sealed_at (server),
// evidence count, report SHA-256, verify URL + QR.
// Disclaimer: "Shared ≠ agreed."
type Spec struct {
	ReportID      string
	VerifyCode    string
	VerifyURL     string
	SealedAt      string
	EvidenceCount int
	ReportSHA256  string
}
