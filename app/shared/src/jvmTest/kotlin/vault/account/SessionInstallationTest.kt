package vault.account

import java.nio.file.Files
import kotlin.test.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class SessionInstallationTest {
    private val fs = FileSystem.SYSTEM
    private fun fixture(block: (Path, SessionInstallation) -> Unit) {
        val dir = Files.createTempDirectory("account-installation-").toString().toPath()
        try { block(dir, SessionInstallation(fs, dir / "installation")) }
        finally { fs.deleteRecursively(dir) }
    }
    @Test fun firstOperationPurgesBeforeMarkerAndRestartPreservesCurrentSession() = fixture { dir, gate ->
        var oldCredential = true
        gate.prepare {
            assertFalse(fs.exists(dir / "installation"))
            oldCredential = false
        }
        assertFalse(oldCredential)
        assertContentEquals(byteArrayOf(1), fs.read(dir / "installation") { readByteArray() })
        SessionInstallation(fs, dir / "installation").prepare { fail("existing installation must not purge") }
    }
    @Test fun missingMarkerAfterReinstallPurgesSurvivingCredential() = fixture { dir, gate ->
        gate.prepare { }
        fs.delete(dir / "installation")
        var purged = false
        SessionInstallation(fs, dir / "installation").prepare { purged = true }
        assertTrue(purged)
    }
    @Test fun failedPurgeNeverAcknowledgesInstallationAndNextAttemptRetries() = fixture { dir, gate ->
        assertFailsWith<SessionFailure> { gate.prepare { throw SessionFailure("storage_locked") } }
        assertFalse(fs.exists(dir / "installation"))
        var retried = false
        gate.prepare { retried = true }
        assertTrue(retried)
        assertTrue(fs.exists(dir / "installation"))
    }
    @Test fun corruptMarkerFailsClosedWithoutSilentlyForgettingSession() = fixture { dir, gate ->
        for (bytes in listOf(byteArrayOf(), byteArrayOf(2), byteArrayOf(1, 1))) {
            fs.write(dir / "installation") { write(bytes) }
            assertFailsWith<SessionFailure> { gate.prepare { fail("corruption requires explicit recovery") } }
        }
    }
}
