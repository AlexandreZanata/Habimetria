package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rsa"
	"crypto/x509"
	"encoding/hex"
)

// Included certificates establish cryptographic consistency, not external trust.
// Neither revocation nor a timestamp-authority claim is fabricated offline.
func checkCertificate(proof *certificateProof, payload []byte, keyID string) (string, bool) {
	if proof == nil || len(proof.Chain) < 1 || len(proof.Chain) > 8 {
		return "", false
	}
	chain := []*x509.Certificate{}
	seen := map[string]bool{}
	for _, encoded := range proof.Chain {
		if len(encoded) > 32768 || seen[encoded] {
			return "", false
		}
		seen[encoded] = true
		der, err := hex.DecodeString(encoded)
		if err != nil {
			return "", false
		}
		cert, err := x509.ParseCertificate(der)
		if err != nil {
			return "", false
		}
		chain = append(chain, cert)
	}
	leaf := chain[0]
	if leaf.IsCA || sha256Hex(leaf.Raw) != keyID || (leaf.KeyUsage != 0 && leaf.KeyUsage&x509.KeyUsageDigitalSignature == 0) {
		return "", false
	}
	for i := 0; i+1 < len(chain); i++ {
		if chain[i].CheckSignatureFrom(chain[i+1]) != nil {
			return "", false
		}
	}
	var algorithm x509.SignatureAlgorithm
	switch key := leaf.PublicKey.(type) {
	case *rsa.PublicKey:
		if proof.Algorithm != "SHA256withRSA" || key.N.BitLen() < 2048 || key.N.BitLen() > 8192 {
			return "", false
		}
		algorithm = x509.SHA256WithRSA
	case *ecdsa.PublicKey:
		if proof.Algorithm != "SHA256withECDSA" || key.Curve != elliptic.P256() {
			return "", false
		}
		algorithm = x509.ECDSAWithSHA256
	default:
		return "", false
	}
	sig, err := hex.DecodeString(proof.Signature)
	if err != nil || leaf.CheckSignature(algorithm, payload, sig) != nil {
		return "", false
	}
	return leaf.Subject.String(), true
}
