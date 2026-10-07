package main

type declarationReference struct {
	ID  string `json:"id"`
	SHA string `json:"sha256"`
}
type declarationIndex struct {
	ReportID      string                 `json:"reportId"`
	PDFSHA        string                 `json:"pdfSha256"`
	ManifestSHA   string                 `json:"manifestSha256"`
	Entries       []declarationReference `json:"entries"`
	FormatVersion int                    `json:"formatVersion"`
	Purpose       string                 `json:"purpose"`
}
type declarationEnvelope struct {
	Payload     string            `json:"payload"`
	Signature   string            `json:"signature"`
	KeyID       string            `json:"keyId"`
	PublicKey   string            `json:"publicKey"`
	Certificate *certificateProof `json:"certificate"`
}
type declarationBody struct {
	ID            string `json:"id"`
	ReportID      string `json:"reportId"`
	PDFSHA        string `json:"pdfSha256"`
	ManifestSHA   string `json:"manifestSha256"`
	Intent        string `json:"intent"`
	ActorName     string `json:"actorName"`
	ActorRole     string `json:"actorRole"`
	Note          string `json:"note"`
	CreatedAt     string `json:"createdAtDevice"`
	PreviousSHA   string `json:"previousSha256"`
	FormatVersion int    `json:"formatVersion"`
	Purpose       string `json:"purpose"`
}
type certificateProof struct {
	Chain     []string `json:"chain"`
	Algorithm string   `json:"algorithm"`
	Signature string   `json:"signature"`
}
type declarationVerdict struct {
	Intent             string `json:"intent"`
	ActorName          string `json:"actorName"`
	KeyID              string `json:"keyId"`
	CertificateSubject string `json:"certificateSubject,omitempty"`
	SignatureValid     bool   `json:"signatureValid"`
	Trust              string `json:"trust"`
	Revocation         string `json:"revocation"`
	TrustedTime        string `json:"trustedTime"`
}
