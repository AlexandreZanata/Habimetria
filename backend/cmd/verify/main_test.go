package main

import (
	"os"
	"path/filepath"
	"testing"
)

func copyTree(t *testing.T, src, dst string) {
	t.Helper()
	entries, err := os.ReadDir(src)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(dst, 0o755); err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		from := filepath.Join(src, entry.Name())
		to := filepath.Join(dst, entry.Name())
		if entry.IsDir() {
			copyTree(t, from, to)
			continue
		}
		raw, err := os.ReadFile(from)
		if err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(to, raw, 0o644); err != nil {
			t.Fatal(err)
		}
	}
}

func fixture(t *testing.T) string {
	t.Helper()
	dst := filepath.Join(t.TempDir(), "package")
	copyTree(t, "../../testdata/fixture-package", dst)
	return dst
}

// The committed fixture was exported by the Kotlin app: this test proves
// the independent Go verifier accepts genuine app output.
func TestFixtureAccepted(t *testing.T) {
	dir := fixture(t)
	v := verifyPackage(dir, map[string]bool{}, map[string]bool{})
	if !v.Accepted {
		t.Fatalf("fixture rejected: %+v", v)
	}
	if v.IssuerTrust != "UNKNOWN" {
		t.Fatalf("fixture trust = %s, want UNKNOWN", v.IssuerTrust)
	}
	if v.EvidencesChecked != 2 {
		t.Fatalf("evidences checked = %d, want 2", v.EvidencesChecked)
	}
}

func TestFixturePinned(t *testing.T) {
	dir := fixture(t)
	v := verifyPackage(dir,
		map[string]bool{"f0529acbc8ee65087a12e93c0f9c74c4638df6ef0d1cd5e5e80c3fa4760cf5e0": true},
		map[string]bool{})
	if !v.Accepted || v.IssuerTrust != "PINNED" {
		t.Fatalf("pinned verdict = %+v", v)
	}
}

func TestTamperedPdfRejected(t *testing.T) {
	dir := fixture(t)
	if err := os.WriteFile(filepath.Join(dir, "report.pdf"), []byte("forged"), 0o644); err != nil {
		t.Fatal(err)
	}
	v := verifyPackage(dir, map[string]bool{}, map[string]bool{})
	if v.Accepted {
		t.Fatal("forged pdf accepted")
	}
	if v.PDF != fileModified || v.Receipt != fileModified {
		t.Fatalf("status = %+v", v)
	}
}

func TestMissingOriginalDistinct(t *testing.T) {
	dir := fixture(t)
	if err := os.Remove(filepath.Join(dir, "originals", "ev0.bin")); err != nil {
		t.Fatal(err)
	}
	v := verifyPackage(dir, map[string]bool{}, map[string]bool{})
	if v.Accepted {
		t.Fatal("package with missing original accepted")
	}
	if len(v.EvidencesMissing) != 1 || v.EvidencesMissing[0] != "ev0" {
		t.Fatalf("missing = %+v", v)
	}
	if len(v.EvidencesModified) != 0 {
		t.Fatalf("modified = %+v", v)
	}
}

func TestSubstitutedSignatureRejected(t *testing.T) {
	dir := fixture(t)
	raw, err := os.ReadFile(filepath.Join(dir, "manifest.sig.json"))
	if err != nil {
		t.Fatal(err)
	}
	// Flip one hex digit of the signature.
	raw[len(raw)-3] ^= 0x01
	if err := os.WriteFile(filepath.Join(dir, "manifest.sig.json"), raw, 0o644); err != nil {
		t.Fatal(err)
	}
	v := verifyPackage(dir, map[string]bool{}, map[string]bool{})
	if v.Accepted || v.SignatureValid {
		t.Fatalf("substituted signature accepted: %+v", v)
	}
}
