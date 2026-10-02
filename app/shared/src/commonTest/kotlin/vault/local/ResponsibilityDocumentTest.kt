package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.*

class ResponsibilityDocumentTest {
    @Test
    fun attachmentAndDimensionsAreFrozenAndTamperingFailsVerification() = runTest {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-responsibility").toString()
        fs.deleteRecursively(dir.toPath()); fs.createDirectories(dir.toPath())
        val session = LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY)
        val repo = session.repository
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua sintética").id).id)
        val room = repo.addRoom(inspection.id, "Sala")
        val photo = session.originals.storeOriginal("photo-document", pdfTestPhoto())
        repo.recordEvidence(inspection.id, room.id, "photo-document.bin", "image/jpeg", photo.size, photo.sha256, evidenceId = "photo-document")
        val file = "$dir/synthetic.pdf".toPath()
        fs.write(file) { writeUtf8("%PDF-synthetic-attachment-bytes") }
        val documents = ResponsibilityDocuments(dir)
        val details = documents.attach(session.db, inspection.id, file.toString(), "test-only.pdf")
        val signer = platformSigner(dir)
        val manifest = (LocalSealer(session.db, session.originals, signer).seal(inspection.id) as SealOutcome.Sealed).manifest
        assertEquals(details.responsibilityDocument, manifestSnapshot(manifest).details.responsibilityDocument)
        assertEquals(ImageDims(80,120), manifestSnapshot(manifest).photoDisplays.getValue("photo-document").dimensions())
        assertFailsWith<SealedModificationException> { documents.attach(session.db, inspection.id, file.toString(), "other.pdf") }
        val generator = ReportGenerator(session.db, dir, session.originals, session.derivatives, signer)
        val renderer = object : PlanRenderer {
            override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
                onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean) = "%PDF-synthetic-report".encodeToByteArray()
        }
        val document = details.responsibilityDocument!!
        val originalAttachment = fs.read(documents.path(document.id).toPath()) { readByteArray() }
        val tamperingRenderer = object : PlanRenderer {
            override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
                onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean): ByteArray {
                fs.write(documents.path(document.id).toPath()) { writeUtf8("%PDF-modified-during-render") }
                return "%PDF-synthetic-report".encodeToByteArray()
            }
        }
        assertFailsWith<IllegalArgumentException> { generator.generate(inspection.id, tamperingRenderer) }
        assertEquals(REPORT_FAILED, generator.existingReport(manifest.id)?.status)
        fs.write(documents.path(document.id).toPath()) { write(originalAttachment) }
        val report = generator.generate(inspection.id, renderer)
        val exported = generator.exportPackage(report.id)
        assertTrue(PackageVerifier(signer).verify(exported).accepted())
        fs.write(documents.path(document.id).toPath()) { writeUtf8("%PDF-modified") }
        assertFalse(generator.verify(report.id).allOk())
        fs.delete("$exported/responsibility/${document.id}.bin".toPath())
        assertFalse(PackageVerifier(signer).verify(exported).accepted())
        assertEquals(vault.local.db.InspectionDeleteOutcome.Deleted, session.inspectionRemoval.delete(inspection.id))
        assertFalse(fs.exists(documents.path(document.id).toPath()))
        assertFalse(fs.exists(exported.toPath()))
        session.close()
    }
}
