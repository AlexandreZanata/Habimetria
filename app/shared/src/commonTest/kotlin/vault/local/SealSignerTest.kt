package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var signerSeq = 0

private fun signerDir(): String {
    signerSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-signer-$signerSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir
}

class SealSignerTest {

    @Test
    fun signVerifyRoundtripWithStableKey() {
        val dir = signerDir()
        val signer = platformSigner(dir)
        assertEquals(64, signer.keyId.length)
        assertEquals(65, signer.publicKeyUncompressed().size)

        val message = "manifest-bytes".encodeToByteArray()
        val signature = signer.sign(message)
        assertEquals(64, signature.size)
        assertTrue(signer.verify(signer.publicKeyUncompressed(), message, signature))

        // Same install, new instance: the key persists.
        val reopened = platformSigner(dir)
        assertEquals(signer.keyId, reopened.keyId)
        assertTrue(reopened.verify(signer.publicKeyUncompressed(), message, signature))
    }

    @Test
    fun tamperingInvalidatesVerification() {
        val signer = platformSigner(signerDir())
        val message = "manifest-bytes".encodeToByteArray()
        val signature = signer.sign(message)
        val tampered = message.copyOf().also { it[0] = (it[0] + 1).toByte() }
        assertFalse(signer.verify(signer.publicKeyUncompressed(), tampered, signature))
        val flipped = signature.copyOf().also { it[10] = (it[10] + 1).toByte() }
        assertFalse(signer.verify(signer.publicKeyUncompressed(), message, flipped))
        assertFalse(signer.verify(signer.publicKeyUncompressed(), message, ByteArray(64)))
    }

    @Test
    fun derRawConversionsRoundtrip() {
        val signer = platformSigner(signerDir())
        val raw = signer.sign("probe".encodeToByteArray())
        assertContentEquals(raw, derToRaw(rawToDer(raw)))
    }

    @Test
    fun fingerprintRejectsMalformedKeys() {
        var failed = false
        try {
            keyFingerprint(ByteArray(64))
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
