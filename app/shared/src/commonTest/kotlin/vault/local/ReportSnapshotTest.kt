package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.*

private class SnapshotRenderer : PlanRenderer {
    val texts = mutableListOf<String>()
    var calls = 0
    override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean): ByteArray {
        calls++
        for (page in pages) for (item in page.items) {
            if (item is PageItem.Text) texts.add(item.text)
            if (item is PageItem.Photo) assertNotNull(imageDimensions(loadPhoto(item.entry.evidenceId)!!))
        }
        return "snapshot-pdf".encodeToByteArray()
    }
}

class ReportSnapshotTest {
    @Test fun liveEditsAndSubstitutedPreviewsCannotChangeSealedContent() = runTest {
        val (db, session, signer) = sealedHarness(1)
        val property = db.propertyDao().listAll().single()
        db.propertyDao().update(property.copy(address = "UNSEALED ADDRESS"))
        val inspection = db.inspectionDao().listAll().single()
        val room = db.roomDao().listForInspection(inspection.id).single()
        db.roomDao().update(room.copy(label = "UNSEALED ROOM"))
        FileSystem.SYSTEM.createDirectories(session.derivatives.previewPath("ev0").toPath().parent!!)
        FileSystem.SYSTEM.write(session.derivatives.previewPath("ev0").toPath()) { writeUtf8("forged preview") }
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val renderer = SnapshotRenderer()
        val report = generator.generate(inspection.id, renderer)
        assertTrue(renderer.texts.any { it == "Rua PDF" })
        assertFalse(renderer.texts.any { "UNSEALED" in it })
        assertEquals(report, generator.generate(inspection.id, renderer))
        assertEquals(1, renderer.calls)
        assertTrue(generator.verify(report.id).allOk())
        db.close()
    }

    @Test fun sameSizeOriginalTamperingAndDeletedReceiptFailClosed() = runTest {
        val (db, session, signer) = sealedHarness(1)
        val inspection = db.inspectionDao().listAll().single().id
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val report = generator.generate(inspection, SnapshotRenderer())
        FileSystem.SYSTEM.delete(generator.receiptPath(report.id).toPath())
        assertFalse(generator.verify(report.id).receiptOk)
        assertFailsWith<IllegalArgumentException> { generator.generate(inspection, SnapshotRenderer()) }
        val path = session.originals.originalPath("ev0").toPath()
        val bytes = FileSystem.SYSTEM.read(path) { readByteArray() }
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        FileSystem.SYSTEM.write(path) { write(bytes) }
        assertFalse(generator.verify(report.id).originalsIntact)
        db.close()
    }

    @Test fun historicalPublicKeyVerifiesAndExportsAfterKeyRotation() = runTest {
        val (db, session, signer) = sealedHarness(1)
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val report = generator.generate(db.inspectionDao().listAll().single().id, SnapshotRenderer())
        val rotated = platformSigner("${session.filesDir}/rotated-key")
        val verifier = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, rotated)
        assertTrue(verifier.verify(report.id).allOk())
        val exported = verifier.exportPackage(report.id)
        assertTrue(PackageVerifier(rotated).verify(exported, pinnedKeys = setOf(signer.keyId)).accepted())
        db.close()
    }
}
