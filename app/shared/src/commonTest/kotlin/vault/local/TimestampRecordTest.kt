package vault.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimestampRecordTest {
    @Test
    fun absentOfflineAndExternalAfterExplicitRegistration() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            assertFalse(timestampPresent(session.filesDir, report.id))
            assertEquals(CERT_TIME_ABSENT, resolveTrustedTime(false))
            assertFailsWith<IllegalArgumentException> {
                registerTimestampToken(session.filesDir, report, ByteArray(64) { 1 }, "ACT", false)
            }
            val record = registerTimestampToken(
                session.filesDir, report, ByteArray(64) { it.toByte() }, "ACT Exemplo", true,
            )
            assertEquals(report.pdfSha256, record.pdfSha256)
            assertEquals(EXTERNAL_SIGNATURE_UNVERIFIED, record.verification)
            assertTrue(timestampPresent(session.filesDir, report.id))
            assertEquals(CERT_TIME_EXTERNAL, resolveTrustedTime(true))
        } finally {
            session.close()
        }
    }
}
