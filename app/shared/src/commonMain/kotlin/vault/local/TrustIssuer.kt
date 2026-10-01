package vault.local

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import vault.local.db.TrustedIssuer
import vault.local.db.VaultDatabase

const val TRUST_PINNED = "PINNED"
const val TRUST_CERTIFIED = "CERTIFIED"
const val PUBLISHER_CERT_PURPOSE = "publisher-cert/v1"

// Publisher certificate: binds an installation key to a publisher id,
// signed by the publisher root. The root is provided by the caller at
// verification time (pinned out-of-band, enrollment outside the offline
// journey). No production root is hardcoded anywhere.
data class PublisherCert(
    val installerKeyId: String,
    val publisherId: String,
    val issuedAtDevice: String,
    val signature: String,
)

fun publisherCertBytes(installerKeyId: String, publisherId: String, issuedAtDevice: String): ByteArray =
    buildString {
        append("{\"installerKeyId\":")
        append(quoteJsonString(installerKeyId))
        append(",\"issuedAtDevice\":")
        append(quoteJsonString(issuedAtDevice))
        append(",\"publisherId\":")
        append(quoteJsonString(publisherId))
        append(",\"purpose\":")
        append(quoteJsonString(PUBLISHER_CERT_PURPOSE))
        append("}")
    }.encodeToByteArray()

fun issuePublisherCert(
    rootSigner: InstallationSigner,
    installerKeyId: String,
    publisherId: String,
): PublisherCert {
    val issuedAt = deviceNowIso()
    val signature = rootSigner.sign(publisherCertBytes(installerKeyId, publisherId, issuedAt))
    return PublisherCert(
        installerKeyId, publisherId, issuedAt,
        signature.toByteString().hex(),
    )
}

fun verifyPublisherCert(
    rootPublicKey: ByteArray,
    cert: PublisherCert,
    crypto: InstallationSigner,
): Boolean =
    try {
        crypto.verify(
            rootPublicKey,
            publisherCertBytes(cert.installerKeyId, cert.publisherId, cert.issuedAtDevice),
            cert.signature.decodeHex().toByteArray(),
        )
    } catch (_: Exception) {
        false
    }

// TrustStore: explicit user trust only. PINNED after comparing the key
// fingerprint out-of-band; CERTIFIED after a valid publisher cert.
// Imported packages can never add trust by themselves.
class TrustStore(private val db: VaultDatabase) {

    suspend fun trust(keyId: String, label: String, source: String = TRUST_PINNED) {
        require(source == TRUST_PINNED || source == TRUST_CERTIFIED) { "unknown source" }
        keyFingerprintFormat(keyId)
        db.trustDao().upsert(TrustedIssuer(keyId, label, source, deviceNowIso()))
    }

    suspend fun revoke(keyId: String) {
        db.trustDao().delete(keyId)
    }

    suspend fun isTrusted(keyId: String): Boolean = db.trustDao().get(keyId) != null

    suspend fun trustedKeys(): Set<String> =
        db.trustDao().list().map { it.keyId }.toSet()

    suspend fun certifiedKeys(): Set<String> =
        db.trustDao().list().filter { it.source == TRUST_CERTIFIED }.map { it.keyId }.toSet()

    suspend fun list(): List<TrustedIssuer> = db.trustDao().list()

    // Promotes an installer to CERTIFIED when its publisher certificate
    // verifies against the caller-provided root. Returns false otherwise
    // and records nothing.
    suspend fun certify(
        cert: PublisherCert,
        rootPublicKey: ByteArray,
        crypto: InstallationSigner,
        label: String,
    ): Boolean {
        if (!verifyPublisherCert(rootPublicKey, cert, crypto)) return false
        trust(cert.installerKeyId, label, TRUST_CERTIFIED)
        return true
    }

    private fun keyFingerprintFormat(keyId: String) {
        require(keyId.length == 64 && keyId.all { it in '0'..'9' || it in 'a'..'f' }) {
            "keyId must be 64 lowercase hex chars"
        }
    }
}
