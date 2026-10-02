package vault.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase

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
    private val artifacts = ReportArtifacts(filesDir, fs)
    private val keys = SignerMaterial(filesDir, fs)
    private val sources = ReportSources(originals, fs)
    fun reportsDir(): String = artifacts.directory()
    fun pdfPath(reportId: String): String = artifacts.pdf(reportId)
    fun receiptPath(reportId: String): String = artifacts.receipt(reportId)
    suspend fun existingReport(manifestId: String): LocalReport? = db.reportDao().getByManifest(manifestId)
    suspend fun versions(manifestId: String): List<LocalReport> = db.reportDao().listForManifest(manifestId)
    suspend fun manifestFor(inspectionId: String) = db.manifestDao().getByInspection(inspectionId)
    suspend fun reportById(reportId: String) = db.reportDao().get(reportId)

    suspend fun exportPackage(reportId: String): String = lock.withLock {
        require(verify(reportId).allOk()) { "report verification failed" }
        PackageExporter(db, filesDir, originals, signer, fs).export(reportId)
    }

    suspend fun exportZip(reportId: String): String = lock.withLock {
        require(verify(reportId).allOk()) { "report verification failed" }
        val dir = PackageExporter(db, filesDir, originals, signer, fs).export(reportId)
        val destination = "$filesDir/vault/packages/$reportId.zip"
        val staged = "$destination.checked"
        try {
            packageZip(dir, staged, fs)
            val imported = importPackageZip(staged, filesDir, fs)
            try { require(PackageVerifier(signer, fs).verify(imported).accepted()) { "exported ZIP verification failed" } }
            finally { fs.deleteRecursively(imported.toPath()) }
            fs.atomicMove(staged.toPath(), destination.toPath())
            destination
        } catch (e: Exception) { fs.delete(staged.toPath(), mustExist = false); throw e }
    }

    // Explicit removal deletes this artifact; later generation has a new ID.
    // Neither originals nor the immutable Manifest is rewritten.
    suspend fun deleteReport(reportId: String): ReportDeleteOutcome = lock.withLock {
        db.reportDao().get(reportId) ?: return@withLock ReportDeleteOutcome.Missing
        fs.delete(pdfPath(reportId).toPath(), mustExist = false)
        fs.delete(receiptPath(reportId).toPath(), mustExist = false)
        db.reportDao().deleteById(reportId)
        ReportDeleteOutcome.Deleted
    }

    suspend fun generate(
        inspectionId: String,
        renderer: PlanRenderer,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
        newVersion: Boolean = false,
    ): LocalReport = lock.withLock {
        val manifest = manifestFor(inspectionId) ?: throw LocalValidationException("seal the inspection first")
        val prior = existingReport(manifest.id)
        val existing = if (newVersion && prior?.status == REPORT_READY) null else prior
        if (existing?.status == REPORT_READY) {
            require(verify(existing.id).allOk()) { "existing finalized report was modified; cannot overwrite" }
            return@withLock existing
        }
        val publicKey = keys.key(manifest.signerKeyId, signer)
        require(LocalSealer(db, originals, signer, fs).verifyManifest(manifest, publicKey)) { "invalid manifest signature" }
        val snapshot = manifestSnapshot(manifest)
        snapshot.details.responsibilityDocument?.let { ResponsibilityDocuments(filesDir, fs).verified(it) }
        require(manifest.signerKeyId == signer.keyId) { "sealing key unavailable for new issuance" }
        val report = existing?.copy(status = REPORT_RENDERING) ?: LocalReport(
            id = newLocalId("rep"), inspectionId = inspectionId, manifestId = manifest.id,
            manifestSha256 = manifest.canonicalSha256, status = REPORT_RENDERING,
            pdfSha256 = null, pdfFileName = null, receiptSignature = null,
            signerKeyId = signer.keyId, algorithm = SEAL_ALGORITHM,
            hardwareBacked = signer.hardwareBacked, createdAtDevice = deviceNowIso(), completedAtDevice = null, versionNumber = (versions(manifest.id).maxOfOrNull { it.versionNumber } ?: 0) + 1,
        )
        if (existing == null) db.reportDao().insert(report) else db.reportDao().update(report)
        try {
            val rows = snapshot.evidences.associateBy { it.id }
            val dimensions = rows.mapValues { (_, evidence) ->
                if (isCancelled()) throw RenderCancelledException()
                val actual = sources.metadata(evidence)
                snapshot.photoDisplays[evidence.id]?.let { require(it == actual) { "display metadata mismatch" } }
                actual.dimensions()
            }
            val model = buildPdfModel(report.id, snapshot.address, snapshot,
                manifest.id, manifest.canonicalSha256, manifest.signerKeyId) { roomId ->
                snapshotCoverage(snapshot, snapshot.rooms.first { it.id == roomId })
            }
            val pages = paginateModel(model, renderer) { dimensions.getValue(it) }
            val bytes = renderer.render(pages, { id -> sources.photo(rows.getValue(id)) }, onProgress, isCancelled)
            if (isCancelled()) throw RenderCancelledException()
            require(bytes.isNotEmpty() && bytes.size <= MAX_ORIGINAL_BYTES) { "invalid PDF output size" }
            artifacts.publish(pdfPath(report.id), bytes)
            val hash = artifacts.hash(pdfPath(report.id))
            val receipt = receiptBytes(report.id, manifest.id, manifest.canonicalSha256, hash, signer.keyId)
            val signature = signer.sign(receipt).toByteString().hex()
            artifacts.publish(receiptPath(report.id),
                ("{\"receipt\":" + receipt.decodeToString() + ",\"signature\":" + quote(signature) + "}").encodeToByteArray())
            val ready = report.copy(status = REPORT_READY, pdfSha256 = hash,
                pdfFileName = "${report.id}.pdf", receiptSignature = signature, completedAtDevice = deviceNowIso())
            require(artifacts.receiptValid(ready, signer, publicKey)) { "final receipt verification failed" }
            // Recheck source bytes after drawing before announcing final success.
            snapshot.evidences.forEach { sources.original(it) }
            snapshot.details.responsibilityDocument?.let { ResponsibilityDocuments(filesDir, fs).verified(it) }
            db.reportDao().update(ready)
            ready
        } catch (e: Exception) {
            db.reportDao().update(report.copy(status = REPORT_FAILED))
            throw e
        }
    }

    suspend fun verify(reportId: String): ReportVerification {
        val report = db.reportDao().get(reportId) ?: throw LocalValidationException("report not found")
        val manifest = db.manifestDao().get(report.manifestId) ?: return ReportVerification(false, false, false, false)
        val manifestOk = runCatching {
            report.manifestSha256 == manifest.canonicalSha256 && LocalSealer(db, originals, signer, fs)
                .verifyManifest(manifest, keys.key(manifest.signerKeyId, signer))
        }.getOrDefault(false)
        val pdfIntact = runCatching { artifacts.hash(pdfPath(report.id)) == report.pdfSha256 }.getOrDefault(false)
        val receiptOk = runCatching {
            artifacts.receiptValid(report, signer, keys.key(report.signerKeyId, signer)) && manifestOk
        }.getOrDefault(false)
        val originalsIntact = runCatching {
            require(manifestOk)
            val snapshot = manifestSnapshot(manifest)
            snapshot.evidences.forEach { sources.original(it) }
            snapshot.details.responsibilityDocument?.let { ResponsibilityDocuments(filesDir, fs).verified(it) }
            true
        }.getOrDefault(false)
        return ReportVerification(pdfIntact, manifestOk, receiptOk, originalsIntact)
    }

    private companion object { val lock = Mutex() }
}
