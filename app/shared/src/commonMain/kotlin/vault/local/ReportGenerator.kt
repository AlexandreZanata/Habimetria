package vault.local

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase

// Generated-report lifecycle: generate → verify → delete. Moved out of
// ReportPipeline.kt (size limit); pipeline keeps receipt/verification
// types. Deleting a generated PDF never touches the sealed Manifest or
// the inspection: "Gerar PDF" rebuilds it on demand.
sealed interface ReportDeleteOutcome {
    data object Deleted : ReportDeleteOutcome
    data object Missing : ReportDeleteOutcome
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

    suspend fun reportById(reportId: String) = db.reportDao().get(reportId)

    suspend fun exportPackage(reportId: String): String {
        val report = db.reportDao().get(reportId)
            ?: throw LocalValidationException("report not found")
        val exporter = PackageExporter(db, filesDir, originals, signer, fs)
        return exporter.export(report.id)
    }

    // Removes the generated row plus its PDF and receipt files. The
    // sealed Manifest, inspection and originals stay intact, so the
    // report regenerates byte-identically. Idempotent: repeats are Missing.
    suspend fun deleteReport(reportId: String): ReportDeleteOutcome {
        db.reportDao().get(reportId) ?: return ReportDeleteOutcome.Missing
        fs.delete(pdfPath(reportId).toPath(), mustExist = false)
        fs.delete(receiptPath(reportId).toPath(), mustExist = false)
        db.reportDao().deleteById(reportId)
        return ReportDeleteOutcome.Deleted
    }

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
