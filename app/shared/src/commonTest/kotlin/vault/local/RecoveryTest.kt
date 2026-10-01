package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import vault.queue.CaptureStore
import vault.queue.QueueItem
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private var recoverySeq = 0

private fun recoveryDir(): String {
    recoverySeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-rec-$recoverySeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir
}

// Disk fills mid-write: the original must not exist half-written, and
// the interruption must surface as an orphan tmp on next reconcile.
private class FillingFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
    override fun sink(file: Path, mustCreate: Boolean): okio.Sink {
        throw IOException("no space left on device")
    }
}

class RecoveryTest {

    @Test
    fun diskFullLeavesNoHalfOriginal() {
        val dir = recoveryDir()
        val store = OriginalStore(FillingFileSystem(FileSystem.SYSTEM), dir)
        assertFailsWith<IOException> {
            store.storeOriginal("ev1", "bytes".encodeToByteArray())
        }
        assertFalse(FileSystem.SYSTEM.exists(store.originalPath("ev1").toPath()))
    }

    @Test
    fun corruptQueueSurfacesInsteadOfSilentEmpty() = runTest {
        val dir = recoveryDir()
        val queue = CaptureStore(FileSystem.SYSTEM, "$dir/queue")
        FileSystem.SYSTEM.createDirectories("$dir/queue".toPath())
        FileSystem.SYSTEM.write("$dir/queue/queue.json".toPath()) { writeUtf8("{corrupt") }

        // Legacy lenient path stays for the old upload flow.
        assertTrue(queue.load().isEmpty())
        // Migration and startup refuse to hide it.
        assertFailsWith<Exception> { queue.loadStrict() }

        val session = openLocalSession(dir, SyncMode.LOCAL_ONLY)
        val startup = session.startup()
        assertNull(startup.migration)
        assertTrue(!startup.migrationError.isNullOrBlank())
        session.close()
    }

    @Test
    fun validQueueStillMigratesAfterStrictness() = runTest {
        val dir = recoveryDir()
        val photo = "$dir/photo.jpg"
        FileSystem.SYSTEM.write(photo.toPath()) { writeUtf8("bytes") }
        CaptureStore(FileSystem.SYSTEM, "$dir/queue")
            .enqueue(QueueItem("q1", "server-ins", "server-room", "Sala", photo))

        val session = openLocalSession(dir, SyncMode.LOCAL_ONLY)
        val startup = session.startup()
        assertEquals(1, startup.migration?.imported)
        assertNull(startup.migrationError)
        session.close()
    }
}
