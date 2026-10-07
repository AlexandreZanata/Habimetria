package vault.local

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun minimalPdf(suffix: String = ""): ByteArray =
    ("%PDF-1.7\n1 0 obj\n<<>>\nendobj\n$suffix\n%%EOF").encodeToByteArray()

class PadesReadinessTest {
    @Test
    fun validPdfPassesAndOthersFail() {
        assertTrue(isPadesReadyPdf(minimalPdf("test")))
        assertFalse(isPadesReadyPdf("code-test-artifact".encodeToByteArray()))
        assertFalse(isPadesReadyPdf(ByteArray(0)))
        assertFalse(isPadesReadyPdf("%PDF-".encodeToByteArray()))
        assertFalse(isPadesReadyPdf("%PDF-1.7 without eof".encodeToByteArray()))
    }
}
