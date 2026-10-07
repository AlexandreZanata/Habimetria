package main

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
)

func TestForgedFingerprintCannotAcquirePinnedTrust(t *testing.T) {
	dir := fixture(t)
	path := filepath.Join(dir, "signer.json")
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	var signer signerFile
	if err := json.Unmarshal(raw, &signer); err != nil {
		t.Fatal(err)
	}
	signer.KeyID = "forged-pinned-key"
	raw, err = json.Marshal(signer)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, raw, 0o644); err != nil {
		t.Fatal(err)
	}
	verdict := verifyPackage(dir, map[string]bool{signer.KeyID: true}, nil)
	if verdict.Accepted || verdict.SignatureValid || verdict.IssuerTrust != "UNKNOWN" {
		t.Fatalf("forged identity accepted: %+v", verdict)
	}
}

func TestUnsupportedReceiptVersionRejected(t *testing.T) {
	dir := fixture(t)
	path := filepath.Join(dir, "receipt.json")
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	var receipt receiptFile
	if err := json.Unmarshal(raw, &receipt); err != nil {
		t.Fatal(err)
	}
	receipt.Receipt.FormatVersion = 999
	raw, err = json.Marshal(receipt)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, raw, 0o644); err != nil {
		t.Fatal(err)
	}
	if verifyPackage(dir, nil, nil).Accepted {
		t.Fatal("unsupported version accepted")
	}
}

func TestSymlinkCannotReadOutsidePackage(t *testing.T) {
	dir := fixture(t)
	path := filepath.Join(dir, "originals", "ev0.bin")
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	outside := filepath.Join(t.TempDir(), "outside.bin")
	if err := os.WriteFile(outside, raw, 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.Remove(path); err != nil {
		t.Fatal(err)
	}
	if err := os.Symlink(outside, path); err != nil {
		t.Fatal(err)
	}
	if verifyPackage(dir, nil, nil).Accepted {
		t.Fatal("symlink escape accepted")
	}
}
