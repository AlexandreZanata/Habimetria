package vault.local

// Revocation stays UNKNOWN unless fresh external evidence is presented.
// Offline generation and verification never contact OCSP/CRL responders,
// so no GOOD/REVOKED verdict is manufactured. An explicit online check
// belongs to the holder's external verifier (for example ITI VALIDAR);
// its result is recorded there, never inferred here.
const val CERT_REVOCATION_GOOD = "GOOD"
const val CERT_REVOCATION_REVOKED = "REVOKED"

fun revocationStatus(explicitOnlineEvidence: String? = null): String {
    if (explicitOnlineEvidence == CERT_REVOCATION_GOOD || explicitOnlineEvidence == CERT_REVOCATION_REVOKED) {
        return explicitOnlineEvidence
    }
    return CERT_REVOCATION_UNKNOWN
}
