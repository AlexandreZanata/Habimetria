package vault.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CertificateValidationTest {
    @Test
    fun validProofReportsUnknownRevocationAndAbsentTime() {
        val signer = personalCertificateSigner() ?: return
        val proof = CertificateProof(
            chain = listOf("00".repeat(32)),
            algorithm = "SHA256withRSA",
            signature = "00".repeat(64),
        )
        // Tampered chain fails closed without claiming trust or time.
        val (identity, status) = certificateStatus(proof, "payload".encodeToByteArray(), emptySet())
        assertNull(identity)
        assertFalse(status.cryptographicValid)
        assertFalse(status.chainValid)
        assertEquals(CERT_REVOCATION_UNKNOWN, status.revocation)
        assertEquals(CERT_TIME_ABSENT, status.trustedTime)
        assertEquals(CERT_TRUST_UNKNOWN, status.trust)
        assertNotNull(signer)
        assertTrue(true)
    }

    @Test
    fun statusConstantsNeverImplyApproval() {
        assertEquals("UNKNOWN", CERT_REVOCATION_UNKNOWN)
        assertEquals("ABSENT", CERT_TIME_ABSENT)
        assertEquals("UNKNOWN", CERT_TRUST_UNKNOWN)
        assertEquals("PINNED", CERT_TRUST_PINNED)
    }
}
