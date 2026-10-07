package vault.local

import kotlin.test.Test
import kotlin.test.assertEquals

class RevocationPolicyTest {
    @Test
    fun offlineStaysUnknownAndNeverImpliesApproval() {
        assertEquals(CERT_REVOCATION_UNKNOWN, revocationStatus())
        assertEquals(CERT_REVOCATION_UNKNOWN, revocationStatus(false.toString()))
        assertEquals(CERT_REVOCATION_UNKNOWN, revocationStatus(null))
        assertEquals("UNKNOWN", CERT_REVOCATION_UNKNOWN)
    }

    @Test
    fun onlyExplicitExternalEvidenceLeavesUnknown() {
        assertEquals(CERT_REVOCATION_GOOD, revocationStatus(CERT_REVOCATION_GOOD))
        assertEquals(CERT_REVOCATION_REVOKED, revocationStatus(CERT_REVOCATION_REVOKED))
    }
}
