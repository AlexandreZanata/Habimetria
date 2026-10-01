// Command verify is the independent offline verifier for local evidence
// packages (see docs/03-architecture/offline-package.md). It shares no
// code with the Kotlin app: same published format, separate
// implementation, separate runtime.
//
// Usage: verify [-trust keyId,...] [-certified keyId,...] [-format text|json] <package-dir>
//
// Exit 0 when the package is accepted, 1 when rejected, 2 on usage errors.
package main

import (
	"bytes"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"math/big"
	"os"
	"path/filepath"
	"strings"
)

type receiptBody struct {
	ReportID      string `json:"reportId"`
	ManifestID    string `json:"manifestId"`
	ManifestSHA   string `json:"manifestSha256"`
	PDFSHA        string `json:"pdfSha256"`
	SignerKeyID   string `json:"signerKeyId"`
}

type receiptFile struct {
	Receipt   receiptBody `json:"receipt"`
	Signature string      `json:"signature"`
}

type manifestSig struct {
	KeyID     string `json:"keyId"`
	Algorithm string `json:"algorithm"`
	Signature string `json:"signature"`
}

type signerFile struct {
	KeyID         string `json:"keyId"`
	PublicKey     string `json:"publicKey"`
	HardwareBacked bool   `json:"hardwareBacked"`
}

type manifestEvidence struct {
	ID     string `json:"id"`
	SHA256 string `json:"sha256"`
}

type manifestDoc struct {
	Evidences []manifestEvidence `json:"evidences"`
}

type verdict struct {
	PDF               string   `json:"pdf"`
	Receipt           string   `json:"receipt"`
	Manifest          string   `json:"manifest"`
	EvidencesChecked  int      `json:"evidencesChecked"`
	EvidencesMissing  []string `json:"evidencesMissing"`
	EvidencesModified []string `json:"evidencesModified"`
	SignatureValid    bool     `json:"signatureValid"`
	IssuerTrust       string   `json:"issuerTrust"`
	Accepted          bool     `json:"accepted"`
}

const (
	fileOK       = "OK"
	fileMissing  = "MISSING"
	fileModified = "MODIFIED"
)

// canonicalReceipt rebuilds the exact signed bytes: fixed field order,
// matching the Kotlin receiptBytes builder.
func canonicalReceipt(r receiptBody) ([]byte, error) {
	var buf bytes.Buffer
	buf.WriteString(`{"algorithm":"ECDSA-P256-SHA256-raw64","formatVersion":1,`)
	writeField(&buf, "manifestId", r.ManifestID)
	writeField(&buf, "manifestSha256", r.ManifestSHA)
	writeField(&buf, "pdfSha256", r.PDFSHA)
	buf.WriteString(`"purpose":"local-receipt/v1",`)
	writeField(&buf, "reportId", r.ReportID)
	buf.WriteString(`"signerKeyId":`)
	writeJSONString(&buf, r.SignerKeyID)
	buf.WriteString(`}`)
	return buf.Bytes(), nil
}

func writeField(buf *bytes.Buffer, name, value string) {
	buf.WriteByte('"')
	buf.WriteString(name)
	buf.WriteString(`":`)
	writeJSONString(buf, value)
	buf.WriteByte(',')
}

func writeJSONString(buf *bytes.Buffer, s string) {
	buf.WriteByte('"')
	for _, r := range s {
		switch r {
		case '"':
			buf.WriteString(`\"`)
		case '\\':
			buf.WriteString(`\\`)
		case '\n':
			buf.WriteString(`\n`)
		case '\r':
			buf.WriteString(`\r`)
		case '\t':
			buf.WriteString(`\t`)
		default:
			if r < 0x20 {
				fmt.Fprintf(buf, `\u%04x`, r)
			} else {
				buf.WriteRune(r)
			}
		}
	}
	buf.WriteByte('"')
}

func sha256Hex(data []byte) string {
	sum := sha256.Sum256(data)
	return hex.EncodeToString(sum[:])
}

func parsePubKey(hexKey string) (*ecdsa.PublicKey, error) {
	raw, err := hex.DecodeString(hexKey)
	if err != nil {
		return nil, err
	}
	x, y := elliptic.Unmarshal(elliptic.P256(), raw)
	if x == nil {
		return nil, fmt.Errorf("invalid P-256 public key")
	}
	return &ecdsa.PublicKey{Curve: elliptic.P256(), X: x, Y: y}, nil
}

func verifyRaw(pub *ecdsa.PublicKey, message []byte, sigHex string) bool {
	raw, err := hex.DecodeString(sigHex)
	if err != nil || len(raw) != 64 {
		return false
	}
	r := new(big.Int).SetBytes(raw[:32])
	s := new(big.Int).SetBytes(raw[32:])
	digest := sha256.Sum256(message)
	return ecdsa.Verify(pub, digest[:], r, s)
}

func readFile(dir, name string) ([]byte, bool) {
	data, err := os.ReadFile(filepath.Join(dir, name))
	if err != nil {
		return nil, false
	}
	return data, true
}

func verifyPackage(dir string, trusted, certified map[string]bool) verdict {
	v := verdict{
		PDF: fileMissing, Receipt: fileMissing, Manifest: fileMissing,
		EvidencesMissing: []string{}, EvidencesModified: []string{},
		IssuerTrust: "UNKNOWN",
	}

	pdfBytes, pdfFound := readFile(dir, "report.pdf")
	receiptRaw, receiptFound := readFile(dir, "receipt.json")
	var receipt receiptFile
	receiptOK := receiptFound && json.Unmarshal(receiptRaw, &receipt) == nil

	manifestBytes, manifestFound := readFile(dir, "manifest.json")
	sigRaw, sigFound := readFile(dir, "manifest.sig.json")
	var sig manifestSig
	sigOK := sigFound && json.Unmarshal(sigRaw, &sig) == nil

	signerRaw, signerFound := readFile(dir, "signer.json")
	var signer signerFile
	signerOK := signerFound && json.Unmarshal(signerRaw, &signer) == nil

	if !pdfFound {
		v.PDF = fileMissing
	} else if !receiptOK {
		v.PDF = fileMissing
		v.Receipt = fileMissing
	} else if receipt.Receipt.PDFSHA == sha256Hex(pdfBytes) {
		v.PDF = fileOK
		v.Receipt = fileOK
	} else {
		v.PDF = fileModified
		v.Receipt = fileModified
	}
	if receiptFound && !receiptOK {
		v.Receipt = fileModified
	}

	if !manifestFound || !sigOK {
		v.Manifest = fileMissing
	} else if receiptOK && sha256Hex(manifestBytes) == receipt.Receipt.ManifestSHA &&
		sig.KeyID == receipt.Receipt.SignerKeyID {
		v.Manifest = fileOK
	} else {
		v.Manifest = fileModified
	}

	pub, pubErr := parsePubKey(signer.PublicKey)
	manifestValid := manifestFound && sigOK && signerOK && pubErr == nil &&
		verifyRaw(pub, manifestBytes, sig.Signature)
	receiptValid := receiptOK && signerOK && pubErr == nil &&
		func() bool {
			canonical, _ := canonicalReceipt(receipt.Receipt)
			return verifyRaw(pub, canonical, receipt.Signature)
		}()
	v.SignatureValid = manifestValid && receiptValid

	if manifestFound {
		var doc manifestDoc
		if json.Unmarshal(manifestBytes, &doc) == nil {
			v.EvidencesChecked = len(doc.Evidences)
			for _, ev := range doc.Evidences {
				raw, found := readFile(dir, filepath.Join("originals", ev.ID+".bin"))
				switch {
				case !found:
					v.EvidencesMissing = append(v.EvidencesMissing, ev.ID)
				case sha256Hex(raw) != ev.SHA256:
					v.EvidencesModified = append(v.EvidencesModified, ev.ID)
				}
			}
		}
	}

	if signerOK {
		switch {
		case certified[signer.KeyID]:
			v.IssuerTrust = "CERTIFIED"
		case trusted[signer.KeyID]:
			v.IssuerTrust = "PINNED"
		}
	}

	v.Accepted = v.PDF == fileOK && v.Receipt == fileOK && v.Manifest == fileOK &&
		len(v.EvidencesMissing) == 0 && len(v.EvidencesModified) == 0 && v.SignatureValid
	return v
}

func splitSet(s string) map[string]bool {
	out := map[string]bool{}
	for _, part := range strings.Split(s, ",") {
		part = strings.TrimSpace(part)
		if part != "" {
			out[part] = true
		}
	}
	return out
}

func main() {
	trustFlag := flag.String("trust", "", "comma-separated trusted key fingerprints (PINNED)")
	certFlag := flag.String("certified", "", "comma-separated certified key fingerprints")
	format := flag.String("format", "text", "output format: text or json")
	flag.Parse()
	if flag.NArg() != 1 {
		fmt.Fprintln(os.Stderr, "usage: verify [-trust k,...] [-certified k,...] [-format text|json] <package-dir>")
		os.Exit(2)
	}
	v := verifyPackage(flag.Arg(0), splitSet(*trustFlag), splitSet(*certFlag))
	if *format == "json" {
		raw, _ := json.MarshalIndent(v, "", "  ")
		fmt.Println(string(raw))
	} else {
		fmt.Printf("pdf: %s\nreceipt: %s\nmanifest: %s\n", v.PDF, v.Receipt, v.Manifest)
		fmt.Printf("evidences: %d checked, %d missing, %d modified\n",
			v.EvidencesChecked, len(v.EvidencesMissing), len(v.EvidencesModified))
		fmt.Printf("signature: %v\nissuer trust: %s\n", v.SignatureValid, v.IssuerTrust)
		fmt.Printf("verdict: %s\n", map[bool]string{true: "ACCEPT", false: "REJECT"}[v.Accepted])
	}
	if !v.Accepted {
		os.Exit(1)
	}
}
