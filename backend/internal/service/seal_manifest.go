package service

import (
	"crypto/rand"
	"encoding/json"
	"fmt"
	"time"

	"vault/internal/store/postgres"
)

// ManifestVersion pins the canonical manifest schema.
const ManifestVersion = 1

// VerifyCodeAlphabet avoids ambiguous chars (no 0/O/1/I/L).
const VerifyCodeAlphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

// VerifyCodeLength is the public code length.
const VerifyCodeLength = 10

type manifestLine struct {
	Sequence         int     `json:"sequence"`
	EvidenceID       string  `json:"evidence_id"`
	SHA256           string  `json:"sha256"`
	Mime             string  `json:"mime"`
	Size             int64   `json:"size"`
	RoomID           *string `json:"room_id"`
	ReceivedAtServer string  `json:"received_at_server"`
}

type manifestDoc struct {
	Version       int            `json:"version"`
	InspectionID  string         `json:"inspection_id"`
	SealedAt      string         `json:"sealed_at"`
	EvidenceCount int            `json:"evidence_count"`
	Evidences     []manifestLine `json:"evidences"`
}

// BuildManifest renders canonical manifest JSON: struct-ordered fields,
// evidences sorted by sequence (store guarantees order). Deterministic:
// same evidence set + sealed_at always yields identical bytes.
func BuildManifest(inspectionID string, sealedAt time.Time, evidences []postgres.SealEvidence) ([]byte, error) {
	doc := manifestDoc{
		Version: ManifestVersion, InspectionID: inspectionID,
		SealedAt:      sealedAt.UTC().Format(time.RFC3339),
		EvidenceCount: len(evidences), Evidences: []manifestLine{},
	}
	for _, e := range evidences {
		room := e.RoomID
		doc.Evidences = append(doc.Evidences, manifestLine{
			Sequence: e.Sequence, EvidenceID: e.EvidenceID, SHA256: e.SHA256,
			Mime: e.Mime, Size: e.Size, RoomID: room,
			ReceivedAtServer: e.ReceivedAtServer.UTC().Format(time.RFC3339),
		})
	}
	body, err := json.Marshal(doc)
	if err != nil {
		return nil, fmt.Errorf("marshal manifest: %w", err)
	}
	return body, nil
}

// GenerateVerifyCode returns a random 10-char public code.
func GenerateVerifyCode() (string, error) {
	out := make([]byte, VerifyCodeLength)
	var buf [VerifyCodeLength]byte
	if _, err := rand.Read(buf[:]); err != nil {
		return "", fmt.Errorf("crypto rand: %w", err)
	}
	for i := range out {
		out[i] = VerifyCodeAlphabet[int(buf[i])%len(VerifyCodeAlphabet)]
	}
	return string(out), nil
}
