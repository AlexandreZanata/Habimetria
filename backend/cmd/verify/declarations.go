package main

import (
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"unicode/utf16"
)

var declarationID = regexp.MustCompile(`^[A-Za-z0-9_-]{1,128}$`)
var declarationHash = regexp.MustCompile(`^[0-9a-f]{64}$`)

func checkDeclarations(dir string, receipt receiptBody, trusted map[string]bool) ([]declarationVerdict, bool) {
	results := []declarationVerdict{}
	indexRaw, exists := readFile(dir, "declarations-index.json")
	folder := filepath.Join(dir, "declarations")
	if !receipt.DeclarationsRequired {
		_, err := os.Lstat(folder)
		return results, !exists && os.IsNotExist(err)
	}
	var envelope declarationEnvelope
	if !exists || len(indexRaw) > 1048576 || json.Unmarshal(indexRaw, &envelope) != nil {
		return results, false
	}
	signerRaw, found := readFile(dir, "signer.json")
	var signer signerFile
	if !found || json.Unmarshal(signerRaw, &signer) != nil || signer.KeyID != receipt.SignerKeyID {
		return results, false
	}
	pub, err := parsePubKey(signer.PublicKey)
	if err != nil || !verifyRaw(pub, []byte(envelope.Payload), envelope.Signature) {
		return results, false
	}
	var index declarationIndex
	if json.Unmarshal([]byte(envelope.Payload), &index) != nil || index.FormatVersion != 1 || index.Purpose != "local-share-index/v1" ||
		index.ReportID != receipt.ReportID || index.PDFSHA != receipt.PDFSHA || index.ManifestSHA != receipt.ManifestSHA || len(index.Entries) > 32 {
		return results, false
	}
	entries, listErr := os.ReadDir(folder)
	if listErr != nil && !os.IsNotExist(listErr) {
		return results, false
	}
	if len(entries) != len(index.Entries) {
		return results, false
	}
	expected := map[string]bool{}
	for _, ref := range index.Entries {
		if !declarationID.MatchString(ref.ID) || !declarationHash.MatchString(ref.SHA) || expected[ref.ID+".json"] {
			return results, false
		}
		expected[ref.ID+".json"] = true
	}
	for _, entry := range entries {
		if !expected[entry.Name()] {
			return results, false
		}
	}
	previous := ""
	for _, ref := range index.Entries {
		raw, found := readFile(dir, filepath.Join("declarations", ref.ID+".json"))
		var file declarationEnvelope
		if !found || len(raw) > 262144 || sha256Hex(raw) != ref.SHA || json.Unmarshal(raw, &file) != nil || len(file.Payload) > 40000 {
			return results, false
		}
		var body declarationBody
		if json.Unmarshal([]byte(file.Payload), &body) != nil || !validDeclaration(body) || body.ID != ref.ID || body.ReportID != receipt.ReportID ||
			body.PDFSHA != receipt.PDFSHA || body.ManifestSHA != receipt.ManifestSHA || body.PreviousSHA != previous {
			return results, false
		}
		v := declarationVerdict{Intent: body.Intent, ActorName: body.ActorName, KeyID: file.KeyID, Trust: "UNKNOWN", Revocation: "UNKNOWN", TrustedTime: "UNKNOWN"}
		if file.Certificate != nil {
			if file.PublicKey != "" || file.Signature != file.Certificate.Signature {
				return results, false
			}
			v.CertificateSubject, v.SignatureValid = checkCertificate(file.Certificate, []byte(file.Payload), file.KeyID)
		} else {
			key, err := parsePubKey(file.PublicKey)
			bytes, hexErr := hex.DecodeString(file.PublicKey)
			v.SignatureValid = err == nil && hexErr == nil && sha256Hex(bytes) == file.KeyID && verifyRaw(key, []byte(file.Payload), file.Signature)
		}
		if !v.SignatureValid {
			return results, false
		}
		if trusted[file.KeyID] {
			v.Trust = "PINNED"
		}
		results = append(results, v)
		previous = ref.SHA
	}
	return results, true
}

func validDeclaration(body declarationBody) bool {
	length := func(s string) int { return len(utf16.Encode([]rune(s))) }
	intent := body.Intent == "AUTHOR" || body.Intent == "RECEIVED" || body.Intent == "AGREED" || body.Intent == "DISAGREED"
	return declarationID.MatchString(body.ID) && declarationID.MatchString(body.ReportID) &&
		body.FormatVersion == 1 && body.Purpose == "local-share-declaration/v1" && intent &&
		declarationHash.MatchString(body.PDFSHA) && declarationHash.MatchString(body.ManifestSHA) &&
		(body.PreviousSHA == "" || declarationHash.MatchString(body.PreviousSHA)) &&
		length(body.ActorName) <= 200 && length(body.ActorRole) <= 100 && length(body.Note) <= 10000 &&
		length(body.CreatedAt) >= 1 && length(body.CreatedAt) <= 100 && (body.Intent != "DISAGREED" || strings.TrimSpace(body.Note) != "")
}
