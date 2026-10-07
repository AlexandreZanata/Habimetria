package main

type receiptBody struct {
	DeclarationsRequired bool   `json:"declarationsRequired"`
	Algorithm            string `json:"algorithm"`
	FormatVersion        int    `json:"formatVersion"`
	Purpose              string `json:"purpose"`
	ReportID             string `json:"reportId"`
	ManifestID           string `json:"manifestId"`
	ManifestSHA          string `json:"manifestSha256"`
	PDFSHA               string `json:"pdfSha256"`
	SignerKeyID          string `json:"signerKeyId"`
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
	KeyID          string `json:"keyId"`
	PublicKey      string `json:"publicKey"`
	HardwareBacked bool   `json:"hardwareBacked"`
}

type manifestEvidence struct {
	ID     string `json:"id"`
	SHA256 string `json:"sha256"`
	Size   int64  `json:"size"`
}

type manifestDoc struct {
	FormatVersion int    `json:"formatVersion"`
	Purpose       string `json:"purpose"`
	ReportDetails struct {
		ResponsibilityDocument *manifestEvidence `json:"responsibilityDocument"`
	} `json:"reportDetails"`
	Evidences []manifestEvidence `json:"evidences"`
}

type verdict struct {
	DeclarationsIntact bool                 `json:"declarationsIntact"`
	Declarations       []declarationVerdict `json:"declarations"`
	PDF                string               `json:"pdf"`
	Receipt            string               `json:"receipt"`
	Manifest           string               `json:"manifest"`
	EvidencesChecked   int                  `json:"evidencesChecked"`
	EvidencesMissing   []string             `json:"evidencesMissing"`
	EvidencesModified  []string             `json:"evidencesModified"`
	SignatureValid     bool                 `json:"signatureValid"`
	IssuerTrust        string               `json:"issuerTrust"`
	Accepted           bool                 `json:"accepted"`
}

const (
	fileOK       = "OK"
	fileMissing  = "MISSING"
	fileModified = "MODIFIED"
)
