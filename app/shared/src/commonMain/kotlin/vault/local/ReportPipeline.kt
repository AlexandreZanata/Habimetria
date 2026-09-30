package vault.local

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase

const val RECEIPT_FORMAT_VERSION = 1
const val RECEIPT_PURPOSE = "local-receipt/v1"
const val REPORT_RENDERING = "RENDERING"
const val REPORT_READY = "READY"
const val REPORT_FAILED = "FAILED"

class IncompleteReportException(val missing: List<String>) :
    IllegalStateException("missing originals for ${missing.size} sealed evidences")

// Detached receipt: binds Report, Manifest and the exact PDF bytes.
// The PDF never embeds its own whole-file hash; integrity lives here.
data class LocalReceipt(
    val reportId: String,
    val manifestId: String,
    val manifestSha256: String,
    val pdfSha256: String,
    val signerKeyId: String,
    val signature: String,
)

fun receiptBytes(
    reportId: String,
    manifestId: String,
    manifestSha256: String,
    pdfSha256: String,
    signerKeyId: String,
): ByteArray = buildString {
    append("{\"algorithm\":")
    append(quote(SEAL_ALGORITHM))
    append(",\"formatVersion\":")
    append(RECEIPT_FORMAT_VERSION)
    append(",\"manifestId\":")
    append(quote(manifestId))
    append(",\"manifestSha256\":")
    append(quote(manifestSha256))
    append(",\"pdfSha256\":")
    append(quote(pdfSha256))
    append(",\"purpose\":")
    append(quote(RECEIPT_PURPOSE))
    append(",\"reportId\":")
    append(quote(reportId))
    append(",\"signerKeyId\":")
    append(quote(signerKeyId))
    append("}")
}.encodeToByteArray()

private fun quote(raw: String): String = buildString {
    append('"')
    for (ch in raw) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u" + ch.code.toString(16).padStart(4, '0'))
            } else {
                append(ch)
            }
        }
    }
    append('"')
}

data class ReportVerification(
    val pdfIntact: Boolean,
    val manifestOk: Boolean,
    val receiptOk: Boolean,
) {
    fun allOk(): Boolean = pdfIntact && manifestOk && receiptOk
}

class ReportGenerator(
    private val db: VaultDatabase,
    private val filesDir: String,
    private val originals: OriginalStore,
    private val derivatives: DerivativeStore,
    private val signer: InstallationSigner,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    private val coverage = VaultCoverage(db)

    fun reportsDir(): String = "$filesDir/vault/reports"

    fun pdfPath(reportId: String): String = "${reportsDir()}/$reportId.pdf"

    fun receiptPath(reportId: String): String = "${reportsDir()}/$reportId-receipt.json"

    suspend fun existingReport(manifestId: String): LocalReport? =
        db.reportDao().getByManifest(manifestId)

    // UI entry points stay on this class so screens never touch the
    // database type directly (keeps the UI module Room-free).
    suspend fun manifestFor(inspectionId: String) =
        db.manifestDao().getByInspection(inspectionId)

    // Finalizes in architecture order: render snapshot → save bytes →
    // hash → sign detached receipt → commit. Cancellation and missing
    // originals fail the job without touching SEALED.
    suspend fun generate(
        inspectionId: String,
        renderer: PlanRenderer,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): LocalReport {
        val manifest = db.manifestDao().getByInspection(inspectionId)
            ?: throw LocalValidationException("seal the inspection first")
        var report = db.reportDao().getByManifest(manifest.id)
        if (report == null) {
            report = LocalReport(
                id = newLocalId("rep"), inspectionId = inspectionId, manifestId = manifest.id,
                manifestSha256 = manifest.canonicalSha256, status = REPORT_RENDERING,
                pdfSha256 = null, pdfFileName = null, receiptSignature = null,
                signerKeyId = signer.keyId, algorithm = SEAL_ALGORITHM,
                hardwareBacked = signer.hardwareBacked, createdAtDevice = deviceNowIso(),
                completedAtDevice = null,
            )
            db.reportDao().insert(report)
        } else {
            report = report.copy(status = REPORT_RENDERING)
            db.reportDao().update(report)
        }
        try {
            // Pre-check: originals may vanish after sealing (disk loss).
            val rows = db.evidenceDao().listForInspection(inspectionId)
            val missing = rows.filterNot { evidence ->
                fs.exists(originals.originalPath(evidence.id).toPath())
            }.map { it.id }
            if (missing.isNotEmpty()) throw IncompleteReportException(missing)

            val model = buildModel(inspectionId, report.id, manifest.canonicalSha256, manifest.id)
            val pages = paginateModel(model) { evidenceId ->
                try {
                    val row = rows.firstOrNull { it.id == evidenceId } ?: return@paginateModel null
                    val bytes = fs.read(originals.originalPath(row.id).toPath()) { readByteArray() }
                    imageDimensions(bytes)
                } catch (_: Exception) {
                    null
                }
            }
            val absent = mutableListOf<String>()
            val pdfBytes = renderer.render(
                pages = pages,
                loadPhoto = { evidenceId ->
                    try {
                        val preview = derivatives.previewPath(evidenceId).toPath()
                        if (fs.exists(preview)) {
                            fs.read(preview) { readByteArray() }
                        } else {
                            fs.read(originals.originalPath(evidenceId).toPath()) { readByteArray() }
                        }
                    } catch (_: Exception) {
                        absent.add(evidenceId)
                        null
                    }
                },
                onProgress = onProgress,
                isCancelled = isCancelled,
            )
            if (absent.isNotEmpty()) throw IncompleteReportException(absent.distinct())

            fs.createDirectories(reportsDir().toPath())
            val tmp = "${pdfPath(report.id)}.tmp".toPath()
            fs.write(tmp) { write(pdfBytes) }
            fs.atomicMove(tmp, pdfPath(report.id).toPath())
            val pdfHash = pdfBytes.toByteString().sha256().hex()
            val receipt = receiptBytes(report.id, manifest.id, manifest.canonicalSha256, pdfHash, signer.keyId)
            val signature = signer.sign(receipt).toByteString().hex()
            fs.write(receiptPath(report.id).toPath()) {
                writeUtf8(
                    "{\"receipt\":" + receipt.decodeToString() +
                        ",\"signature\":" + quote(signature) + "}",
                )
            }
            val done = report.copy(
                status = REPORT_READY, pdfSha256 = pdfHash,
                pdfFileName = "${report.id}.pdf", receiptSignature = signature,
                completedAtDevice = deviceNowIso(),
            )
            db.reportDao().update(done)
            return done
        } catch (e: RenderCancelledException) {
            db.reportDao().update(report.copy(status = REPORT_FAILED))
            throw e
        } catch (e: Exception) {
            db.reportDao().update(report.copy(status = REPORT_FAILED))
            throw e
        }
    }

    suspend fun verify(reportId: String): ReportVerification {
        val report = db.reportDao().get(reportId)
            ?: throw LocalValidationException("report not found")
        val manifest = db.manifestDao().get(report.manifestId)
            ?: return ReportVerification(false, false, false)
        val pdfIntact = try {
            val bytes = fs.read(pdfPath(report.id).toPath()) { readByteArray() }
            bytes.toByteString().sha256().hex() == report.pdfSha256
        } catch (_: Exception) {
            false
        }
        val manifestOk = manifest.canonicalSha256 == report.manifestSha256 &&
            LocalSealer(db, originals, signer, fs).verifyManifest(manifest, signer.publicKeyUncompressed())
        val receiptOk = try {
            val receipt = receiptBytes(
                report.id, manifest.id, manifest.canonicalSha256,
                report.pdfSha256 ?: "", signer.keyId,
            )
            signer.verify(
                signer.publicKeyUncompressed(), receipt,
                (report.receiptSignature ?: "").decodeHex().toByteArray(),
            ) && manifestOk
        } catch (_: Exception) {
            false
        }
        return ReportVerification(pdfIntact, manifestOk, receiptOk)
    }

    private suspend fun buildModel(
        inspectionId: String,
        reportId: String,
        manifestHash: String,
        manifestId: String,
    ): PdfModel {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        val tenancy = db.tenancyDao().get(inspection.tenancyId)
            ?: throw LocalValidationException("tenancy not found")
        val property = db.propertyDao().get(tenancy.propertyId)
            ?: throw LocalValidationException("property not found")
        val rooms = db.roomDao().listForInspection(inspectionId)
        val percents = rooms.associate { it.id to coverage.roomCoverage(it.id).percent() }
        return buildPdfModel(
            reportId = reportId,
            address = property.address,
            input = ManifestInput(
                propertyId = property.id,
                address = property.address,
                tenancy = tenancy,
                inspection = inspection,
                rooms = rooms,
                evidences = db.evidenceDao().listForInspection(inspectionId),
                findings = rooms.flatMap { db.findingDao().listForRoom(it.id) },
                sealedAtDevice = inspection.sealedAtDevice ?: "",
            ),
            manifestId = manifestId,
            manifestSha256 = manifestHash,
            signerKeyId = signer.keyId,
            coverageOf = { roomId -> percents[roomId] ?: 0 },
        )
    }
}
