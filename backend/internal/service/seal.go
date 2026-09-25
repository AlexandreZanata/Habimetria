package service

import "errors"

// Seal transitions DRAFT -> SEALED. Terminal; no rollback.
// Steps: validate payment, freeze, build manifest.json sorted by
// sequence, SHA-256, HMAC-sign, create report + verify_code.
var ErrAlreadySealed = errors.New("inspection already sealed")

type SealInput struct {
	InspectionID string
	PaymentProof string
}

type SealOutput struct {
	ManifestSHA256 string
	ReportID       string
	VerifyCode     string
}
