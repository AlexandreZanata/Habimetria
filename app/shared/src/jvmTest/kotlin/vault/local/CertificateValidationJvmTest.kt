package vault.local

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CertificateValidationJvmTest {
    @Test fun syntheticHolderReportsSeparateChainTrustAndTime() {
        val directory = Files.createTempDirectory("vault-cert-status-test").toFile()
        try {
            val file = syntheticCertificate(directory)
            val signer = requireNotNull(personalCertificateSigner())
            val payload = "status declaration".encodeToByteArray()
            val proof = signer.sign(file.path, "test-password".toCharArray(), payload)
            val (identity, status) = certificateStatus(proof, payload, emptySet())
            assertNotNull(identity)
            assertTrue(status.cryptographicValid)
            assertTrue(status.chainValid)
            assertFalse(status.expiredAtDevice)
            assertFalse(status.notYetValidAtDevice)
            assertEquals(CERT_REVOCATION_UNKNOWN, status.revocation)
            assertEquals(CERT_TIME_ABSENT, status.trustedTime)
            assertEquals(CERT_TRUST_UNKNOWN, status.trust)

            val pinned = certificateStatus(proof, payload, setOf(identity.fingerprint))
            assertEquals(CERT_TRUST_PINNED, pinned.second.trust)

            val altered = certificateStatus(proof, "changed".encodeToByteArray(), emptySet())
            assertEquals(null, altered.first)
            assertFalse(altered.second.cryptographicValid)

            val expiredDir = File(directory, "expired-status").apply { mkdirs() }
            val expired = syntheticCertificate(expiredDir, startDate = "-2d", validity = 1)
            // Expired holder cannot sign (device validity check at signing).
            try {
                signer.sign(expired.path, "test-password".toCharArray(), payload)
                assertTrue(false, "expired signing must fail")
            } catch (_: IllegalArgumentException) {
                assertTrue(true)
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
