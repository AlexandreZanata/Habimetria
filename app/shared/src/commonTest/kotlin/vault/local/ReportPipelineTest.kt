package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var pipelineSeq = 0

private class FakeRenderer(
    var cancelAfterPages: Int = Int.MAX_VALUE,
    val seenPhotos: MutableList<String> = mutableListOf(),
) : PlanRenderer {
    var plans = 0
    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray {
        plans = pages.size
        var done = 0
        for (page in pages) {
            if (isCancelled() || done >= cancelAfterPages) throw RenderCancelledException()
            for (item in page.items) {
                if (item is PageItem.Photo) {
                    seenPhotos.add(item.entry.evidenceId)
                    loadPhoto(item.entry.evidenceId)
                }
                if (item is PageItem.Qr) {
                    require(item.content.startsWith("vault-report/v1/"))
                }
            }
            done++
            onProgress(done, pages.size)
        }
        return "pdf-bytes".encodeToByteArray()
    }
}

internal suspend fun sealedHarness(photos: Int = 3): Triple<VaultDatabase, LocalSession, InstallationSigner> {
    pipelineSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-pipe-$pipelineSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
    val signer = platformSigner(dir)
    val repo = session.repository
    val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua PDF").id).id).id
    val room = repo.addRoom(inspection, "Sala")
    repeat(photos) { i ->
        val bytes = pdfTestPhoto()
        val id = "ev$i"
        val stored = session.originals.storeOriginal(id, bytes)
        repo.recordEvidence(
            inspection, room.id, "$id.bin", "image/jpeg",
            stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = id,
        )
    }
    LocalSealer(db, session.originals, signer).seal(inspection)
    return Triple(db, session, signer)
}

class ReportPipelineTest {

    @Test
    fun generateFinalizesBytesHashAndReceipt() = runTest {
        val (db, session, signer) = sealedHarness()
        val inspection = db.inspectionDao().listAll().single().id
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val renderer = FakeRenderer()

        val report = generator.generate(inspection, renderer)
        assertEquals(REPORT_READY, report.status)
        assertTrue(renderer.plans > 0)
        assertEquals(listOf("ev0", "ev1", "ev2"), renderer.seenPhotos)
        assertTrue(FileSystem.SYSTEM.exists(generator.pdfPath(report.id).toPath()))
        assertTrue(FileSystem.SYSTEM.exists(generator.receiptPath(report.id).toPath()))

        val verification = generator.verify(report.id)
        assertTrue(verification.allOk())

        // A finalized Report returns unchanged; the renderer is not invoked again.
        val again = generator.generate(inspection, renderer)
        assertEquals(report.id, again.id)
        assertEquals(REPORT_READY, again.status)
        db.close()
    }

    @Test
    fun cancellationFailsWithoutTouchingSeal() = runTest {
        val (db, session, signer) = sealedHarness()
        val inspection = db.inspectionDao().listAll().single().id
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)

        assertFailsWith<RenderCancelledException> {
            generator.generate(inspection, FakeRenderer(cancelAfterPages = 0), isCancelled = { true })
        }
        val manifest = db.manifestDao().getByInspection(inspection)!!
        val report = generator.existingReport(manifest.id)!!
        assertEquals(REPORT_FAILED, report.status)
        assertEquals(STATUS_SEALED, db.inspectionDao().get(inspection)!!.status)
        db.close()
    }

    @Test
    fun missingOriginalFailsFinalization() = runTest {
        val (db, session, signer) = sealedHarness(photos = 2)
        val inspection = db.inspectionDao().listAll().single().id
        FileSystem.SYSTEM.delete(session.originals.originalPath("ev1").toPath())
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)

        val failure = assertFailsWith<IncompleteReportException> {
            generator.generate(inspection, FakeRenderer())
        }
        assertEquals(listOf("ev1"), failure.missing)
        db.close()
    }

    @Test
    fun tamperedPdfFailsVerification() = runTest {
        val (db, session, signer) = sealedHarness(photos = 1)
        val inspection = db.inspectionDao().listAll().single().id
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val report = generator.generate(inspection, FakeRenderer())

        FileSystem.SYSTEM.write(generator.pdfPath(report.id).toPath()) { writeUtf8("tampered") }
        val verification = generator.verify(report.id)
        assertFalse(verification.pdfIntact)
        assertFalse(verification.allOk())
        assertTrue(verification.manifestOk)
        db.close()
    }
}
