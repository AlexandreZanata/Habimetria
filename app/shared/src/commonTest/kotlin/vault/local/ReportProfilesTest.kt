package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.*

class ReportProfilesTest {
    @Test
    fun technicalInspectionNeedsNoTenancyAndFreezesReviewedDetails() = runTest {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-professional").toString()
        fs.deleteRecursively(dir.toPath()); fs.createDirectories(dir.toPath())
        val session = LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY)
        val inspection = session.repository.createTechnicalInspection(session.repository.createProperty("Rua Técnica").id)
        assertNull(inspection.tenancyId)
        assertEquals(1, session.repository.propertyOverviews().single().draftCount)
        assertEquals(inspection.propertyId, session.overviews.inspectionOverviews().single().propertyId)
        assertFalse(session.propertyRemoval.eligibility(inspection.propertyId!!)?.canDelete ?: true)
        val room = session.repository.addRoom(inspection.id, "Sala")
        val stored = session.originals.storeOriginal("technical-photo", pdfTestPhoto())
        session.repository.recordEvidence(inspection.id, room.id, "technical-photo.bin", "image/jpeg", stored.size,
            stored.sha256, evidenceId = "technical-photo")
        val signer = platformSigner(dir)
        val sealer = LocalSealer(session.db, session.originals, signer)
        assertTrue(SEAL_REPORT_DETAILS in sealer.assess(inspection.id).reasons)
        val details = ReportDetails(profile = "technical", authorName = "Autor de teste", council = "CREA",
            councilState = "MT", registration = "DECLARADO-TESTE", responsibilityType = "ART",
            responsibilityReference = "REFERÊNCIA-TESTE", purpose = "Teste do registro", scope = "Sala",
            methods = "Observação visual", limitations = "Sem ensaios", analysis = "Registro fotográfico",
            conclusions = "Sem conclusão de segurança estrutural", authorAcknowledged = true)
        ReportDetailsRepository(session.db).save(inspection.id, details)
        val manifest = (sealer.seal(inspection.id) as SealOutcome.Sealed).manifest
        assertEquals(details, manifestSnapshot(manifest).details)
        assertNull(manifestSnapshot(manifest).tenancy)
        assertFailsWith<SealedModificationException> { ReportDetailsRepository(session.db).save(inspection.id, details.copy(scope = "Outra")) }
        session.close()
    }

    @Test
    fun newReportVersionPreservesFirstBytesAndReceipt() = runTest {
        val (db, session, signer) = sealedHarness(1)
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val inspectionId = db.inspectionDao().listAll().single().id
        val renderer = object : PlanRenderer {
            override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
                onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean) = "version-${pages.first().header}".encodeToByteArray()
        }
        val first = generator.generate(inspectionId, renderer)
        val bytes = FileSystem.SYSTEM.read(generator.pdfPath(first.id).toPath()) { readByteArray() }
        val second = generator.generate(inspectionId, renderer, newVersion = true)
        assertNotEquals(first.id, second.id)
        assertEquals(2, second.versionNumber)
        assertContentEquals(bytes, FileSystem.SYSTEM.read(generator.pdfPath(first.id).toPath()) { readByteArray() })
        assertTrue(generator.verify(first.id).allOk()); assertTrue(generator.verify(second.id).allOk())
        assertEquals(listOf(second.id, first.id), generator.versions(first.manifestId).map { it.id })
        session.close()
    }
}
