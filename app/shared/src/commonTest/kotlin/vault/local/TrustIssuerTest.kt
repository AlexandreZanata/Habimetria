package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var trustSeq = 0

private suspend fun trustHarness(): Triple<TrustStore, InstallationSigner, String> {
    trustSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-trust-$trustSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return Triple(TrustStore(db), platformSigner(dir), dir)
}

class TrustIssuerTest {

    @Test
    fun pinRevokeRoundtrip() = runTest {
        val (trust, signer, _) = trustHarness()
        assertFalse(trust.isTrusted(signer.keyId))
        assertFailsWith<IllegalArgumentException> { trust.trust("xyz", "bad") }

        trust.trust(signer.keyId, "meu celular")
        assertTrue(trust.isTrusted(signer.keyId))
        assertEquals(setOf(signer.keyId), trust.trustedKeys())
        assertTrue(trust.certifiedKeys().isEmpty())

        trust.revoke(signer.keyId)
        assertFalse(trust.isTrusted(signer.keyId))
    }

    @Test
    fun publisherCertPromotesOnlyWithValidRoot() = runTest {
        val (trust, installer, _) = trustHarness()
        val (_, root, _) = trustHarness()
        val rootPub = root.publicKeyUncompressed()

        val cert = issuePublisherCert(root, installer.keyId, "publisher-acme")
        assertTrue(verifyPublisherCert(rootPub, cert, installer))
        assertTrue(trust.certify(cert, rootPub, installer, "Acme Imobiliária"))
        assertEquals(setOf(installer.keyId), trust.certifiedKeys())

        // Tampered publisher id fails closed and records nothing.
        val (trust2, _, _) = trustHarness()
        assertFalse(trust2.certify(cert.copy(publisherId = "evil"), rootPub, installer, "x"))
        assertTrue(trust2.trustedKeys().isEmpty())

        // Wrong root fails closed.
        val (_, other, _) = trustHarness()
        assertFalse(trust2.certify(cert, other.publicKeyUncompressed(), installer, "x"))
        assertTrue(trust2.trustedKeys().isEmpty())
    }

    @Test
    fun packageTrustLevels() = runTest {
        val (trust, signer, _) = trustHarness()
        val (_, other, _) = trustHarness()
        // Certified implies trusted; pinned stays pinned, unknown unknown.
        trust.trust(signer.keyId, "a")
        assertEquals(setOf(signer.keyId), trust.trustedKeys())
        assertFalse(other.keyId in trust.trustedKeys())
    }
}
