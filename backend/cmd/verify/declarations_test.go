package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func declarationFixture(t *testing.T) (string, receiptBody, *ecdsa.PrivateKey) {
	t.Helper()
	dir := t.TempDir()
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	pub := elliptic.Marshal(key.Curve, key.X, key.Y)
	receipt := receiptBody{FormatVersion: 2, DeclarationsRequired: true, ReportID: "report", PDFSHA: strings.Repeat("a", 64), ManifestSHA: strings.Repeat("b", 64), SignerKeyID: sha256Hex(pub)}
	writeDeclarationJSON(t, dir, "signer.json", signerFile{KeyID: receipt.SignerKeyID, PublicKey: hex.EncodeToString(pub)})
	index := declarationIndex{ReportID: receipt.ReportID, PDFSHA: receipt.PDFSHA, ManifestSHA: receipt.ManifestSHA, FormatVersion: 1, Purpose: "local-share-index/v1"}
	for _, id := range []string{"first", "second"} {
		body := declarationBody{ID: id, ReportID: receipt.ReportID, PDFSHA: receipt.PDFSHA, ManifestSHA: receipt.ManifestSHA, Intent: "AUTHOR", CreatedAt: "device-time", FormatVersion: 1, Purpose: "local-share-declaration/v1"}
		if len(index.Entries) > 0 {
			body.PreviousSHA = index.Entries[len(index.Entries)-1].SHA
		}
		payload, _ := json.Marshal(body)
		file := declarationEnvelope{Payload: string(payload), KeyID: receipt.SignerKeyID, PublicKey: hex.EncodeToString(pub), Signature: rawDeclarationSignature(t, key, payload)}
		raw := writeDeclarationJSON(t, dir, filepath.Join("declarations", id+".json"), file)
		index.Entries = append(index.Entries, declarationReference{ID: id, SHA: sha256Hex(raw)})
	}
	publishDeclarationIndex(t, dir, key, index)
	return dir, receipt, key
}

func writeDeclarationJSON(t *testing.T, dir, name string, value any) []byte {
	t.Helper()
	raw, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	path := filepath.Join(dir, name)
	if err := os.MkdirAll(filepath.Dir(path), 0700); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, raw, 0600); err != nil {
		t.Fatal(err)
	}
	return raw
}
func rawDeclarationSignature(t *testing.T, key *ecdsa.PrivateKey, payload []byte) string {
	t.Helper()
	hash := sha256.Sum256(payload)
	r, s, err := ecdsa.Sign(rand.Reader, key, hash[:])
	if err != nil {
		t.Fatal(err)
	}
	raw := make([]byte, 64)
	r.FillBytes(raw[:32])
	s.FillBytes(raw[32:])
	return hex.EncodeToString(raw)
}
func publishDeclarationIndex(t *testing.T, dir string, key *ecdsa.PrivateKey, index declarationIndex) {
	payload, _ := json.Marshal(index)
	writeDeclarationJSON(t, dir, "declarations-index.json", declarationEnvelope{Payload: string(payload), Signature: rawDeclarationSignature(t, key, payload)})
}

func TestDeclarationIntegrityAndIndependentTrust(t *testing.T) {
	dir, receipt, _ := declarationFixture(t)
	declarations, ok := checkDeclarations(dir, receipt, nil)
	if !ok || len(declarations) != 2 || declarations[0].Trust != "UNKNOWN" {
		t.Fatalf("unexpected declarations %+v", declarations)
	}
	declarations, ok = checkDeclarations(dir, receipt, map[string]bool{receipt.SignerKeyID: true})
	if !ok || declarations[0].Trust != "PINNED" {
		t.Fatal("independent pin ignored")
	}
	receipt.ReportID = "different-report"
	if _, ok := checkDeclarations(dir, receipt, nil); ok {
		t.Fatal("accepted other report")
	}
}
func TestDeclarationOmissionAndAlteration(t *testing.T) {
	for _, change := range []string{"remove-one", "remove-all", "remove-index", "alter", "add"} {
		t.Run(change, func(t *testing.T) {
			dir, receipt, _ := declarationFixture(t)
			switch change {
			case "remove-one":
				os.Remove(filepath.Join(dir, "declarations/first.json"))
			case "remove-all":
				os.RemoveAll(filepath.Join(dir, "declarations"))
			case "remove-index":
				os.Remove(filepath.Join(dir, "declarations-index.json"))
			case "alter":
				os.WriteFile(filepath.Join(dir, "declarations/first.json"), []byte("changed"), 0600)
			case "add":
				os.WriteFile(filepath.Join(dir, "declarations/extra.json"), []byte("extra"), 0600)
			}
			if _, ok := checkDeclarations(dir, receipt, nil); ok {
				t.Fatal("accepted altered set")
			}
		})
	}
}
func TestReorderedSignedIndexDoesNotReorderHistory(t *testing.T) {
	dir, receipt, key := declarationFixture(t)
	raw, _ := os.ReadFile(filepath.Join(dir, "declarations-index.json"))
	var envelope declarationEnvelope
	json.Unmarshal(raw, &envelope)
	var index declarationIndex
	json.Unmarshal([]byte(envelope.Payload), &index)
	index.Entries[0], index.Entries[1] = index.Entries[1], index.Entries[0]
	publishDeclarationIndex(t, dir, key, index)
	if _, ok := checkDeclarations(dir, receipt, nil); ok {
		t.Fatal("accepted inconsistent chain order")
	}
}
func TestLegacyReceiptDoesNotAcceptUnboundDeclarations(t *testing.T) {
	dir, receipt, _ := declarationFixture(t)
	receipt.FormatVersion = 1
	receipt.DeclarationsRequired = false
	if _, ok := checkDeclarations(dir, receipt, nil); ok {
		t.Fatal("accepted unbound declarations")
	}
}
