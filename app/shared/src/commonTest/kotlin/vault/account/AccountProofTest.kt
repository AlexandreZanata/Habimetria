package vault.account

import kotlin.test.*

class AccountProofTest {
    @Test fun acceptsOnlyTheExpectedProofFromLinkOrRawValue() {
        val proof = "A".repeat(43)
        assertEquals(proof, accountProof(proof, "verificar"))
        assertEquals(proof, accountProof("https://example.test/painel/verificar#verificar=$proof", "verificar"))
        assertFailsWith<SessionFailure> { accountProof("https://example.test/#recuperar=$proof", "verificar") }
        assertFailsWith<SessionFailure> { accountProof("https://example.test/#verificar=$proof&extra=1", "verificar") }
        assertFailsWith<SessionFailure> { accountProof("https://example.test/no-proof", "verificar") }
    }
}
