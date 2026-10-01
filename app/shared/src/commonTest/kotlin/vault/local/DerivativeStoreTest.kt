package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeScaler : ImageScaler {
    var calls = 0
    override fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray {
        calls++
        return "scaled-$maxEdge".encodeToByteArray()
    }
}

class DerivativeStoreTest {

    @Test
    fun writesBothDerivativesOnce() {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-deriv").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        val scaler = FakeScaler()
        val store = DerivativeStore(FileSystem.SYSTEM, dir, scaler)
        assertFalse(store.hasDerivatives("e1"))

        val original = "original-bytes".encodeToByteArray()
        store.ensureDerivatives("e1", original)
        assertTrue(store.hasDerivatives("e1"))
        assertEquals(2, scaler.calls)
        assertTrue(
            FileSystem.SYSTEM.read(store.thumbPath("e1").toPath()) { readUtf8() }
                .startsWith("scaled-"),
        )

        // Idempotent: second call leaves files untouched.
        store.ensureDerivatives("e1", original)
        assertEquals(2, scaler.calls)
    }
}
