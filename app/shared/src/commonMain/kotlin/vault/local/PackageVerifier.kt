package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase

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
    ): PackageVerdict = try { verifyChecked(packageDir, pinnedKeys, certifiedKeys) } catch (_: Exception) {
        PackageVerdict(FileStatus.MODIFIED, FileStatus.MODIFIED, FileStatus.MODIFIED, 0, emptyList(), emptyList(), false, IssuerTrust.UNKNOWN)
    }

    private fun verifyChecked(packageDir: String, pinnedKeys: Set<String>, certifiedKeys: Set<String>): PackageVerdict {
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
        val keyBinding = runCatching {
            publicKey != null && keyFingerprint(publicKey) == signerFile?.keyId &&
                signerFile.keyId == manifestSig?.keyId && signerFile.keyId == receiptFile?.receipt?.signerKeyId &&
                manifestSig.algorithm == SEAL_ALGORITHM && receiptFile.receipt.algorithm == SEAL_ALGORITHM &&
                receiptFile.receipt.formatVersion == RECEIPT_FORMAT_VERSION && receiptFile.receipt.purpose == RECEIPT_PURPOSE
        }.getOrDefault(false)
        val manifestValid = keyBinding && manifestBytes != null && manifestSig != null && publicKey != null &&
            signer.verify(
                publicKey,
                manifestBytes.encodeToByteArray(),
                manifestSig.signature.decodeHexSafe(),
            )
        val receiptValid = keyBinding && receiptFile != null && publicKey != null &&
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
            keyBinding && keyId in certifiedKeys -> IssuerTrust.CERTIFIED
            keyBinding && keyId in pinnedKeys -> IssuerTrust.PINNED
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
        require(summary.formatVersion in 1..MANIFEST_FORMAT_VERSION && summary.purpose == MANIFEST_PURPOSE) { "unsupported manifest" }
        require(summary.evidences.size in 1..1000 && summary.evidences.distinctBy { it.id }.size == summary.evidences.size) { "invalid originals" }
        require(summary.evidences.sumOf { it.size } <= MAX_PACKAGE_BYTES) { "package too large" }
        val missing = mutableListOf<String>()
        val modified = mutableListOf<String>()
        for (evidence in summary.evidences) {
            require(safeArtifactId(evidence.id) && evidence.size in 1..MAX_ORIGINAL_BYTES) { "invalid original" }
            val path = dir.resolve("originals/${evidence.id}.bin")
            if (!fs.exists(path)) {
                missing.add(evidence.id)
                continue
            }
            require(fs.canonicalize(path).toString().startsWith(fs.canonicalize(dir).toString() + "/")) { "original outside package" }
            if (fs.metadata(path).size != evidence.size) { modified.add(evidence.id); continue }
            val bytes = fs.read(path) { readByteArray() }
            if (bytes.toByteString().sha256().hex() != evidence.sha256) modified.add(evidence.id)
        }
        return Triple(summary.evidences.size, missing, modified)
    }

    private fun readOrNull(path: okio.Path): ByteArray? =
        if (fs.exists(path)) {
            require(fs.metadata(path).size in 1..MAX_ORIGINAL_BYTES && fs.metadata(path).symlinkTarget == null)
            fs.read(path) { readByteArray() }
        } else null

    private fun readUtf8OrNull(path: okio.Path): String? =
        if (fs.exists(path)) {
            require(fs.metadata(path).size in 1L..8L * 1024 * 1024 && fs.metadata(path).symlinkTarget == null)
            fs.read(path) { readUtf8() }
        } else null

    private fun String.decodeHexSafe(): ByteArray =
        try {
            decodeHex().toByteArray()
        } catch (_: Exception) {
            ByteArray(0)
        }
}
