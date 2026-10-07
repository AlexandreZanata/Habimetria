package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalSigningTest {
    @Test
    fun handoffPreservesReadyAndRegistersDistinctCopy() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            val pdfBytes = FileSystem.SYSTEM.read(generator.pdfPath(report.id).toPath()) { readByteArray() }
            val handoff = prepareExternalHandoff(report, pdfBytes.size.toLong(), EXTERNAL_METHOD_PADES)
            assertEquals(report.id, handoff.reportId)
            assertEquals(report.pdfSha256, handoff.pdfSha256)
            assertTrue(handoff.instructions.contains("VALIDAR"))
            assertTrue(handoff.instructions.contains("does not embed PAdES"))
            assertTrue(handoff.instructions.contains("DocMDP"))

            val govbr = prepareExternalHandoff(report, pdfBytes.size.toLong(), EXTERNAL_METHOD_GOVBR)
            assertTrue(govbr.instructions.contains("Gov.br"))
            assertTrue(govbr.instructions.contains("prata"))
            val stamp = prepareExternalHandoff(report, pdfBytes.size.toLong(), EXTERNAL_METHOD_TIMESTAMP)
            assertTrue(stamp.instructions.contains("RFC 3161"))

            assertFailsWith<IllegalArgumentException> { prepareExternalHandoff(report, pdfBytes.size.toLong(), "fake-method") }

            // Non-PDF bytes are rejected; explicit intent is required.
            assertFailsWith<IllegalArgumentException> {
                registerExternalSignedCopy(session.filesDir, report, EXTERNAL_METHOD_PADES, pdfBytes)
            }
            val pdfSigned = "%PDF-1.7\nexternal-signed-copy\n%%EOF".encodeToByteArray()
            assertFailsWith<IllegalArgumentException> {
                registerExternalSignedCopy(session.filesDir, report, EXTERNAL_METHOD_PADES, pdfSigned, acknowledged = false)
            }
            val record = registerExternalSignedCopy(session.filesDir, report, EXTERNAL_METHOD_PADES, pdfSigned)
            assertEquals(report.pdfSha256, record.originalPdfSha256)
            assertEquals(EXTERNAL_SIGNATURE_UNVERIFIED, record.verification)
            // READY bytes untouched; signed copy lives alongside under a distinct name.
            assertTrue(FileSystem.SYSTEM.read(generator.pdfPath(report.id).toPath()) { readByteArray() }.contentEquals(pdfBytes))
            assertTrue(FileSystem.SYSTEM.exists(externalSignedPath(session.filesDir, report.id, EXTERNAL_METHOD_PADES).toPath()))
            val verified = verifyExternalCopy(session.filesDir, report, EXTERNAL_METHOD_PADES)
            assertEquals(record, verified)
            assertTrue(generator.verify(report.id).allOk())
        } finally {
            session.close()
        }
    }
}
