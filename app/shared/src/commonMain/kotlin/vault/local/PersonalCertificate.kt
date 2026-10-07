package vault.local

import kotlinx.serialization.Serializable

@Serializable
data class CertificateIdentity(val subject: String, val issuer: String, val fingerprint: String,
    val serial: String, val validFrom: String, val validUntil: String)

@Serializable
data class CertificateProof(val chain: List<String>, val algorithm: String, val signature: String)

interface PersonalCertificateSigner {
    fun inspect(path: String, password: CharArray): CertificateIdentity
    fun sign(path: String, password: CharArray, payload: ByteArray): CertificateProof
    fun verify(proof: CertificateProof, payload: ByteArray): CertificateIdentity?
}

// A platform capability, independent from installation signing.
expect fun personalCertificateSigner(): PersonalCertificateSigner?
