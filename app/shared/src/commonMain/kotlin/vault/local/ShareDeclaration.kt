package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class DeclarationIntent { AUTHOR, RECEIVED, AGREED, DISAGREED }

@Serializable
data class ShareDeclarationBody(
    val id: String, val reportId: String, val pdfSha256: String, val manifestSha256: String,
    val intent: DeclarationIntent, val actorName: String, val actorRole: String, val note: String,
    val createdAtDevice: String, val previousSha256: String,
    val formatVersion: Int = 1, val purpose: String = "local-share-declaration/v1",
) {
    fun validate() {
        require(safeArtifactId(id) && safeArtifactId(reportId) && formatVersion == 1 && purpose == "local-share-declaration/v1")
        require(listOf(pdfSha256, manifestSha256).all { it.matches(Regex("[0-9a-f]{64}")) })
        require(previousSha256.isEmpty() || previousSha256.matches(Regex("[0-9a-f]{64}")))
        require(actorName.length <= 200 && actorRole.length <= 100 && note.length <= 10_000 && createdAtDevice.length in 1..100)
        require(intent != DeclarationIntent.DISAGREED || note.isNotBlank()) { "describe the disagreement" }
    }
}

@Serializable
data class ShareDeclarationFile(val payload: String, val keyId: String, val signature: String,
    val publicKey: String = "", val certificate: CertificateProof? = null)

@Serializable
data class DeclarationReference(val id: String, val sha256: String)

@Serializable
data class DeclarationIndex(val reportId: String, val pdfSha256: String, val manifestSha256: String,
    val entries: List<DeclarationReference> = emptyList(), val formatVersion: Int = 1,
    val purpose: String = "local-share-index/v1")

@Serializable
data class DeclarationIndexFile(val payload: String, val signature: String)

data class DeclarationCheck(val intent: DeclarationIntent, val actorName: String, val actorRole: String,
    val note: String, val createdAtDevice: String, val valid: Boolean, val keyId: String,
    val certificate: CertificateIdentity? = null, val trusted: Boolean = false,
    val certificateStatus: CertificateStatus? = null)

internal val declarationJson = Json { encodeDefaults = true }
internal fun declarationPayload(body: ShareDeclarationBody): String = declarationJson.encodeToString(body)
