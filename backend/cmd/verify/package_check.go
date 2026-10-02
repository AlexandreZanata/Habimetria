package main

import (
	"encoding/hex"
	"encoding/json"
	"path/filepath"
	"regexp"
)

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
	publicBytes, hexErr := hex.DecodeString(signer.PublicKey)
	keyBound := signerOK && sigOK && receiptOK && hexErr == nil && sha256Hex(publicBytes) == signer.KeyID &&
		signer.KeyID == sig.KeyID && sig.KeyID == receipt.Receipt.SignerKeyID && sig.Algorithm == "ECDSA-P256-SHA256-raw64" &&
		receipt.Receipt.Algorithm == sig.Algorithm && receipt.Receipt.FormatVersion >= 1 && receipt.Receipt.FormatVersion <= 2 && receipt.Receipt.DeclarationsRequired == (receipt.Receipt.FormatVersion == 2) && receipt.Receipt.Purpose == "local-receipt/v1"
	manifestValid := keyBound && manifestFound && sigOK && signerOK && pubErr == nil &&
		verifyRaw(pub, manifestBytes, sig.Signature)
	receiptValid := keyBound && receiptOK && signerOK && pubErr == nil &&
		func() bool {
			canonical, err := canonicalReceipt(receipt.Receipt)
			return err == nil && verifyRaw(pub, canonical, receipt.Signature)
		}()
	v.SignatureValid = manifestValid && receiptValid

	if manifestFound {
		var doc manifestDoc
		if json.Unmarshal(manifestBytes, &doc) == nil && doc.FormatVersion >= 1 && doc.FormatVersion <= 4 && doc.Purpose == "local-seal/v1" && len(doc.Evidences) > 0 && len(doc.Evidences) <= 1000 {
			v.EvidencesChecked = len(doc.Evidences)
			originals := append([]manifestEvidence{}, doc.Evidences...)
			if doc.ReportDetails.ResponsibilityDocument != nil {
				originals = append(originals, *doc.ReportDetails.ResponsibilityDocument)
			}
			seen := map[string]bool{}
			var total int64
			for _, ev := range originals {
				total += ev.Size
				if !regexp.MustCompile(`^[A-Za-z0-9_-]{1,128}$`).MatchString(ev.ID) || seen[ev.ID] || ev.Size <= 0 || ev.Size > 50*1024*1024 || total > 512*1024*1024 {
					v.SignatureValid = false
					continue
				}
				seen[ev.ID] = true
				folder := "originals"
				if doc.ReportDetails.ResponsibilityDocument != nil && ev.ID == doc.ReportDetails.ResponsibilityDocument.ID {
					folder = "responsibility"
				}
				raw, found := readFile(dir, filepath.Join(folder, ev.ID+".bin"))
				switch {
				case !found:
					v.EvidencesMissing = append(v.EvidencesMissing, ev.ID)
				case int64(len(raw)) != ev.Size || sha256Hex(raw) != ev.SHA256:
					v.EvidencesModified = append(v.EvidencesModified, ev.ID)
				}
			}
		} else {
			v.SignatureValid = false
		}
	}

	if keyBound {
		switch {
		case certified[signer.KeyID]:
			v.IssuerTrust = "CERTIFIED"
		case trusted[signer.KeyID]:
			v.IssuerTrust = "PINNED"
		}
	}

	v.Declarations, v.DeclarationsIntact = checkDeclarations(dir, receipt.Receipt, trusted)
	v.Accepted = v.PDF == fileOK && v.Receipt == fileOK && v.Manifest == fileOK &&
		len(v.EvidencesMissing) == 0 && len(v.EvidencesModified) == 0 && v.SignatureValid && v.DeclarationsIntact
	return v
}
