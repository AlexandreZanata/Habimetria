package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase

@Serializable
internal data class ReceiptFile(
    val receipt: ReceiptBody,
    val signature: String,
)

@Serializable
internal data class ReceiptBody(
    val algorithm: String,
    val formatVersion: Int,
    val purpose: String,
    val reportId: String,
    val manifestId: String,
    val manifestSha256: String,
    val pdfSha256: String,
    val signerKeyId: String,
    val declarationsRequired: Boolean = false,
)

@Serializable
internal data class ManifestSigFile(
    val keyId: String,
    val algorithm: String,
    val signature: String,
)

@Serializable
internal data class SignerFile(
    val keyId: String,
    val publicKey: String,
    val hardwareBacked: Boolean,
)

@Serializable
internal data class ManifestSummary(
    val inspectionId: String = "",
    val formatVersion: Int = 0,
    val purpose: String = "",
    val reportDetails: ReportDetails = ReportDetails(),
    val evidences: List<ManifestEvidence> = emptyList(),
)

@Serializable
internal data class ManifestEvidence(
    val id: String,
    val sha256: String,
    val size: Long,
)

internal val packageJson = Json { ignoreUnknownKeys = true }
