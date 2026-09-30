package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase

// Offline evidence package (directory). The published format both the app
// and the independent verifier implement:
//
//   <package>/report.pdf          final PDF bytes
//   <package>/receipt.json        {"receipt": {...}, "signature": hex}
//   <package>/manifest.json       canonical manifest bytes, verbatim
//   <package>/manifest.sig.json   {"keyId","algorithm","signature"}
//   <package>/signer.json         {"keyId","publicKey","hardwareBacked"}
//   <package>/originals/<id>.bin  immutable originals by evidence id
//
// Identity (QR) never proves integrity: only the receipt's PDF hash and
// the manifest signature do, recomputed from the shipped bytes.
const val PACKAGE_VERSION = 1

enum class FileStatus { OK, MISSING, MODIFIED }

enum class IssuerTrust { UNKNOWN, PINNED, CERTIFIED }

data class PackageVerdict(
    val pdf: FileStatus,
    val receipt: FileStatus,
    val manifest: FileStatus,
    val evidencesChecked: Int,
    val evidencesMissing: List<String>,
    val evidencesModified: List<String>,
    val signatureValid: Boolean,
    val issuerTrust: IssuerTrust,
) {
    fun accepted(): Boolean =
        pdf == FileStatus.OK && receipt == FileStatus.OK && manifest == FileStatus.OK &&
            evidencesMissing.isEmpty() && evidencesModified.isEmpty() && signatureValid
}

@Serializable
private data class ReceiptFile(
    val receipt: ReceiptBody,
    val signature: String,
)

@Serializable
private data class ReceiptBody(
    val reportId: String,
    val manifestId: String,
    val manifestSha256: String,
    val pdfSha256: String,
    val signerKeyId: String,
)

@Serializable
private data class ManifestSigFile(
    val keyId: String,
    val algorithm: String,
    val signature: String,
)

@Serializable
private data class SignerFile(
    val keyId: String,
    val publicKey: String,
    val hardwareBacked: Boolean,
)

@Serializable
private data class ManifestSummary(
    val inspectionId: String = "",
    val evidences: List<ManifestEvidence> = emptyList(),
)

@Serializable
private data class ManifestEvidence(
    val id: String,
    val sha256: String,
)

private val packageJson = Json { ignoreUnknownKeys = true }

class PackageExporter(
    private val db: VaultDatabase,
    private val filesDir: String,
    private val originals: OriginalStore,
    private val signer: InstallationSigner,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    fun packageDir(reportId: String): String = "$filesDir/vault/packages/$reportId"

    // Exports a READY report. Fails closed when anything is missing;
    // never writes a partial package over a previous export.
    suspend fun export(reportId: String): String {
        val report = db.reportDao().get(reportId)
            ?: throw LocalValidationException("report not found")
        require(report.status == REPORT_READY) { "report is ${report.status}, not READY" }
        val manifest = db.manifestDao().get(report.manifestId)
            ?: throw LocalValidationException("manifest not found")
        val pdfBytes = readReportPdf(reportId)
        val dir = packageDir(reportId)
        val staging = "$dir.tmp".toPath()
        fs.deleteRecursively(staging)
        fs.createDirectories(staging)
        fs.write(staging.resolve("report.pdf")) { write(pdfBytes) }
        copyTextFile("$filesDir/vault/reports/$reportId-receipt.json", staging.resolve("receipt.json"))
        fs.write(staging.resolve("manifest.json")) { writeUtf8(manifest.canonicalJson) }
        fs.write(staging.resolve("manifest.sig.json")) {
            writeUtf8(
                "{\"keyId\":\"${manifest.signerKeyId}\"," +
                    "\"algorithm\":\"${manifest.algorithm}\"," +
                    "\"signature\":\"${manifest.signature}\"}",
            )
        }
        val publicKey = signer.publicKeyUncompressed()
        fs.write(staging.resolve("signer.json")) {
            writeUtf8(
                "{\"keyId\":\"${signer.keyId}\"," +
                    "\"publicKey\":\"${publicKey.toByteString().hex()}\"," +
                    "\"hardwareBacked\":${signer.hardwareBacked}}",
            )
        }
        val originalsDir = staging.resolve("originals")
        fs.createDirectories(originalsDir)
        for (evidence in db.evidenceDao().listForInspection(report.inspectionId)) {
            val source = originals.originalPath(evidence.id).toPath()
            if (!fs.exists(source)) throw IncompleteReportException(listOf(evidence.id))
            fs.copy(source, originalsDir.resolve("${evidence.id}.bin"))
        }
        val final = dir.toPath()
        fs.deleteRecursively(final)
        fs.atomicMove(staging, final)
        return dir
    }

    private fun readReportPdf(reportId: String): ByteArray {
        val path = "$filesDir/vault/reports/$reportId.pdf".toPath()
        if (!fs.exists(path)) throw LocalValidationException("report pdf missing")
        return fs.read(path) { readByteArray() }
    }

    private fun copyTextFile(from: String, to: okio.Path) {
        val source = from.toPath()
        if (!fs.exists(source)) throw LocalValidationException("missing $from")
        fs.copy(source, to)
    }
}

class PackageVerifier(
    private val signer: InstallationSigner,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    // Verifies an imported package with no database: recompute everything
    // from shipped bytes. Missing and modified stay distinct. Trust comes
    // only from the caller's pinned/certified sets, never from the package.
    fun verify(
        packageDir: String,
        pinnedKeys: Set<String> = emptySet(),
        certifiedKeys: Set<String> = emptySet(),
    ): PackageVerdict {
        val dir = packageDir.toPath()
        val pdfBytes = readOrNull(dir.resolve("report.pdf"))
        val receiptFile = readUtf8OrNull(dir.resolve("receipt.json"))?.let {
            packageJson.decodeFromString<ReceiptFile>(it)
        }
        val manifestBytes = readUtf8OrNull(dir.resolve("manifest.json"))
        val manifestSig = readUtf8OrNull(dir.resolve("manifest.sig.json"))?.let {
            packageJson.decodeFromString<ManifestSigFile>(it)
        }
        val signerFile = readUtf8OrNull(dir.resolve("signer.json"))?.let {
            packageJson.decodeFromString<SignerFile>(it)
        }

        val receiptStatus = when {
            receiptFile == null || pdfBytes == null -> FileStatus.MISSING
            receiptFile.receipt.pdfSha256 == pdfBytes.toByteString().sha256().hex() -> FileStatus.OK
            else -> FileStatus.MODIFIED
        }
        val manifestStatus = when {
            manifestBytes == null || manifestSig == null -> FileStatus.MISSING
            manifestBytes.encodeToByteArray().toByteString().sha256().hex() ==
                receiptFile?.receipt?.manifestSha256 &&
                manifestSig.keyId == receiptFile.receipt.signerKeyId -> FileStatus.OK
            else -> FileStatus.MODIFIED
        }
        val publicKey = try {
            signerFile?.publicKey?.decodeHex()?.toByteArray()
        } catch (_: Exception) {
            null
        }
        val manifestValid = manifestBytes != null && manifestSig != null && publicKey != null &&
            signer.verify(
                publicKey,
                manifestBytes.encodeToByteArray(),
                manifestSig.signature.decodeHexSafe(),
            )
        val receiptValid = receiptFile != null && publicKey != null &&
            signer.verify(
                publicKey,
                receiptBytes(
                    receiptFile.receipt.reportId, receiptFile.receipt.manifestId,
                    receiptFile.receipt.manifestSha256, receiptFile.receipt.pdfSha256,
                    receiptFile.receipt.signerKeyId,
                ),
                receiptFile.signature.decodeHexSafe(),
            )
        val (checked, missing, modified) = if (manifestBytes != null) {
            checkOriginals(dir, manifestBytes)
        } else {
            Triple(0, emptyList(), emptyList())
        }
        val keyId = signerFile?.keyId
        val trust = when {
            keyId == null -> IssuerTrust.UNKNOWN
            keyId in certifiedKeys -> IssuerTrust.CERTIFIED
            keyId in pinnedKeys -> IssuerTrust.PINNED
            else -> IssuerTrust.UNKNOWN
        }
        return PackageVerdict(
            pdf = if (pdfBytes == null) FileStatus.MISSING else receiptStatus,
            receipt = if (receiptFile == null) FileStatus.MISSING else receiptStatus,
            manifest = manifestStatus,
            evidencesChecked = checked,
            evidencesMissing = missing,
            evidencesModified = modified,
            signatureValid = manifestValid && receiptValid,
            issuerTrust = trust,
        )
    }

    private fun checkOriginals(dir: okio.Path, manifestBytes: String): Triple<Int, List<String>, List<String>> {
        val summary = packageJson.decodeFromString<ManifestSummary>(manifestBytes)
        val missing = mutableListOf<String>()
        val modified = mutableListOf<String>()
        for (evidence in summary.evidences) {
            val path = dir.resolve("originals/${evidence.id}.bin")
            if (!fs.exists(path)) {
                missing.add(evidence.id)
                continue
            }
            val bytes = fs.read(path) { readByteArray() }
            if (bytes.toByteString().sha256().hex() != evidence.sha256) modified.add(evidence.id)
        }
        return Triple(summary.evidences.size, missing, modified)
    }

    private fun readOrNull(path: okio.Path): ByteArray? =
        if (fs.exists(path)) fs.read(path) { readByteArray() } else null

    private fun readUtf8OrNull(path: okio.Path): String? =
        if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    private fun String.decodeHexSafe(): ByteArray =
        try {
            decodeHex().toByteArray()
        } catch (_: Exception) {
            ByteArray(0)
        }
}
