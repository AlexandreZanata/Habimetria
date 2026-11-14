package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"strings"
	"testing"
	"time"

	"vault/internal/store/postgres"
	"vault/internal/testutil"
)

func sealEvidences() []postgres.SealEvidence {
	room := testutil.ID(1)
	ts := testutil.TestTime()
	return []postgres.SealEvidence{
		{Sequence: 1, EvidenceID: "ev-1", SHA256: "aa", Mime: "image/jpeg", Size: 100, RoomID: &room, ReceivedAtServer: ts},
		{Sequence: 2, EvidenceID: "ev-2", SHA256: "bb", Mime: "image/jpeg", Size: 200, ReceivedAtServer: ts},
	}
}

func TestBuildManifestDeterministic(t *testing.T) {
	sealedAt, _ := time.Parse(time.RFC3339, "2026-09-25T12:00:00Z")
	a, err := BuildManifest("ins-1", sealedAt, sealEvidences())
	if err != nil {
		t.Fatal(err)
	}
	b, err := BuildManifest("ins-1", sealedAt, sealEvidences())
	if err != nil {
		t.Fatal(err)
	}
	if string(a) != string(b) {
		t.Fatal("manifest must be deterministic")
	}
	if !strings.Contains(string(a), `"version":1`) || !strings.Contains(string(a), `"evidence_count":2`) {
		t.Fatalf("bad manifest: %s", a)
	}
}

func TestBuildManifestEmptyEvidencesArray(t *testing.T) {
	body, err := BuildManifest("ins-1", time.Now().UTC(), nil)
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(body), `"evidences":[]`) {
		t.Fatalf("empty set must encode as []: %s", body)
	}
}

func TestGenerateVerifyCodeFormat(t *testing.T) {
	for i := 0; i < 20; i++ {
		code, err := GenerateVerifyCode()
		if err != nil {
			t.Fatal(err)
		}
		if len(code) != VerifyCodeLength {
			t.Fatalf("bad length: %q", code)
		}
		for _, c := range code {
			if !strings.ContainsRune(VerifyCodeAlphabet, c) {
				t.Fatalf("bad char %q in %q", c, code)
			}
		}
	}
}

func TestSealHMACVerifies(t *testing.T) {
	svc := NewSeal(nil, testutil.HMACKey())
	sig := svc.sign("manifest-sha")
	mac := hmac.New(sha256.New, []byte(testutil.HMACKey()))
	mac.Write([]byte("manifest-sha"))
	if want := hex.EncodeToString(mac.Sum(nil)); sig != want {
		t.Fatalf("hmac mismatch: %s vs %s", sig, want)
	}
}

func TestSealRejectsInvalidInspection(t *testing.T) {
	svc := NewSeal(nil, testutil.HMACKey())
	_, err := svc.Seal(context.Background(), SealInput{
		InspectionID: "invalid-inspection",
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}
