package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase

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
        require(ReportGenerator(db, filesDir, originals,
            DerivativeStore(fs, filesDir, platformScaler()), signer, fs).verify(reportId).allOk()) {
            "cannot export an altered report"
        }
        val snapshot = manifestSnapshot(manifest)
        val pdfBytes = readReportPdf(reportId)
        val dir = packageDir(reportId)
        if (fs.exists(dir.toPath())) {
            if (PackageVerifier(signer, fs).verify(dir).accepted()) return dir
        }
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
        val publicKey = SignerMaterial(filesDir, fs).key(report.signerKeyId, signer)
        fs.write(staging.resolve("signer.json")) {
            writeUtf8(
                "{\"keyId\":\"${report.signerKeyId}\"," +
                    "\"publicKey\":\"${publicKey.toByteString().hex()}\"," +
                    "\"hardwareBacked\":${report.hardwareBacked}}",
            )
        }
        val originalsDir = staging.resolve("originals")
        fs.createDirectories(originalsDir)
        for (evidence in snapshot.evidences) {
            val source = originals.originalPath(evidence.id).toPath()
            if (!fs.exists(source)) throw IncompleteReportException(listOf(evidence.id))
            val verified = ReportSources(originals, fs).original(evidence)
            fs.write(originalsDir.resolve("${evidence.id}.bin")) { write(verified) }
        }
        snapshot.details.responsibilityDocument?.let { document ->
            val documentsDir = staging.resolve("responsibility")
            fs.createDirectories(documentsDir)
            fs.write(documentsDir.resolve("${document.id}.bin")) { write(ResponsibilityDocuments(filesDir, fs).verified(document)) }
        }
        require(PackageVerifier(signer, fs).verify(staging.toString()).accepted()) { "staged package verification failed" }
        val final = dir.toPath()
        val backup = "$dir.previous".toPath()
        fs.deleteRecursively(backup)
        if (fs.exists(final)) fs.atomicMove(final, backup)
        try { fs.atomicMove(staging, final) }
        catch (e: Exception) { if (fs.exists(backup)) fs.atomicMove(backup, final); throw e }
        fs.deleteRecursively(backup)
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

