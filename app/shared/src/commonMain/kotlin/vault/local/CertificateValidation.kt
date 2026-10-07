package vault.local

import kotlinx.serialization.Serializable

// Separate certificate verdicts. Unavailable external checks are never
// presented as approved: revocation stays UNKNOWN offline and trusted
// time stays ABSENT until an external ACT token is registered.
const val CERT_REVOCATION_UNKNOWN = "UNKNOWN"
const val CERT_TIME_ABSENT = "ABSENT"
const val CERT_TIME_EXTERNAL = "EXTERNAL"
const val CERT_TRUST_UNKNOWN = "UNKNOWN"
const val CERT_TRUST_PINNED = "PINNED"

@Serializable
data class CertificateStatus(
    val cryptographicValid: Boolean,
    val chainValid: Boolean,
    val expiredAtDevice: Boolean,
    val notYetValidAtDevice: Boolean,
    val revocation: String = CERT_REVOCATION_UNKNOWN,
    val trustedTime: String = CERT_TIME_ABSENT,
    val trust: String = CERT_TRUST_UNKNOWN,
)

fun certificateStatus(
    proof: CertificateProof,
    payload: ByteArray,
    pinnedFingerprints: Set<String> = emptySet(),
    timestampPresent: Boolean = false,
): Pair<CertificateIdentity?, CertificateStatus> {
    val signer = personalCertificateSigner()
    val identity = signer?.verify(proof, payload)
    if (identity == null) {
        return null to CertificateStatus(
            cryptographicValid = false,
            chainValid = false,
            expiredAtDevice = false,
            notYetValidAtDevice = false,
        )
    }
    val (expired, notYet) = certificateDeviceValidity(identity.validFrom, identity.validUntil)
    val trust = if (identity.fingerprint in pinnedFingerprints) CERT_TRUST_PINNED else CERT_TRUST_UNKNOWN
    return identity to CertificateStatus(
        cryptographicValid = true,
        chainValid = true,
        expiredAtDevice = expired,
        notYetValidAtDevice = notYet,
        revocation = revocationStatus(),
        trustedTime = resolveTrustedTime(timestampPresent),
        trust = trust,
    )
}

// Device-clock validity only; never a certified time claim.
expect fun certificateDeviceValidity(validFromIso: String, validUntilIso: String): Pair<Boolean, Boolean>
