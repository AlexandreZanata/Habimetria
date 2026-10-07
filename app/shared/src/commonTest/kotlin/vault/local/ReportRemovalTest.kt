package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalManifest
import vault.local.db.LocalReport
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private var reportRemovalSeq = 0

class ReportRemovalTest {

    @Test
    fun deleteReportRemovesRowAndFilesIdempotently() = runTest {
        reportRemovalSeq++
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-report-removal-$reportRemovalSeq").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())
        val session = LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY, fs)
        val generator = ReportGenerator(
            session.db, session.filesDir, session.originals, session.derivatives, platformSigner(dir),
        )

        val repo = session.repository
        val prop = repo.createProperty("Rua A, 1")
        val inspection = repo.createInspection(repo.createTenancy(prop.id).id)
        val manifest = LocalManifest(
            id = "man1", inspectionId = inspection.id, formatVersion = 1,
            canonicalSha256 = "ab".repeat(32), canonicalJson = "{}",
            sealedAtDevice = "2026-01-01T00:00:00", signerKeyId = "k1",
            algorithm = "ed25519", hardwareBacked = false, signature = "sig",
        )
        session.db.manifestDao().insert(manifest)
        session.db.reportDao().insert(
            LocalReport(
                id = "rep1", inspectionId = inspection.id, manifestId = manifest.id,
                manifestSha256 = manifest.canonicalSha256, status = REPORT_READY,
                pdfSha256 = "cd".repeat(32), pdfFileName = "rep1.pdf",
                receiptSignature = "rsig", signerKeyId = "k1", algorithm = "ed25519",
                hardwareBacked = false, createdAtDevice = "2026-01-01T00:00:00",
                completedAtDevice = "2026-01-01T00:00:01",
            ),
        )
        fs.createDirectories(generator.reportsDir().toPath())
        fs.write(generator.pdfPath("rep1").toPath()) { write(byteArrayOf(1, 2, 3)) }
        fs.write(generator.receiptPath("rep1").toPath()) { writeUtf8("{}") }

        assertIs<ReportDeleteOutcome.Deleted>(generator.deleteReport("rep1"))
        assertEquals(null, session.db.reportDao().get("rep1"))
        assertFalse(fs.exists(generator.pdfPath("rep1").toPath()))
        assertFalse(fs.exists(generator.receiptPath("rep1").toPath()))
        // Manifest and inspection survive: the report regenerates on demand.
        assertTrue(session.db.manifestDao().getByInspection(inspection.id) != null)
        assertIs<ReportDeleteOutcome.Missing>(generator.deleteReport("rep1"))
        assertIs<ReportDeleteOutcome.Missing>(generator.deleteReport("rep_missing"))
        session.close()
    }
}
