package vault

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.api.Evidence
import vault.queue.Backoff
import vault.queue.CaptureStore
import vault.queue.EvidenceRemote
import vault.queue.QueueItem
import vault.queue.QueueStatus
import vault.queue.UploadPump
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private var testDirSeq = 0

private fun testStore(): Pair<CaptureStore, String> {
    testDirSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-test-$testDirSeq").toString()
    // Counter restarts every JVM run; drop stale files from previous runs.
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    return CaptureStore(FileSystem.SYSTEM, dir) to dir
}

class CaptureStoreTest {

    @Test
    fun roundtripAndPending() {
        val (store, _) = testStore()
        assertTrue(store.load().isEmpty())
        store.enqueue(QueueItem("l1", "i1", null, "kitchen", "/tmp/a.jpg"))
        store.enqueue(QueueItem("l2", "i1", null, "living", "/tmp/b.jpg"))
        assertEquals(2, store.pending().size)
        store.update("l1") { it.copy(status = QueueStatus.CONFIRMED, evidenceId = "e1") }
        assertEquals(listOf("l2"), store.pending().map { it.localId })
        assertEquals("e1", store.load().first { it.localId == "l1" }.evidenceId)
    }
}

class BackoffTest {

    @Test
    fun exponentialCapped() {
        assertEquals(2_000L, Backoff.delayMs(0))
        assertEquals(4_000L, Backoff.delayMs(1))
        assertEquals(Backoff.CAP_MS, Backoff.delayMs(100))
    }
}

class FakeRemote(var failures: Int = 0) : EvidenceRemote {
    var calls = 0
    override suspend fun uploadPhoto(inspectionId: String, roomId: String?, bytes: ByteArray, mime: String): Evidence {
        calls++
        if (failures > 0) {
            failures--
            throw RuntimeException("offline")
        }
        return Evidence("e$calls", inspectionId, "2026-09-25T10:00:00Z", calls)
    }
}

class UploadPumpTest {

    @Test
    fun confirmsOnAck() = runTest {
        val (store, dir) = testStore()
        val fs = FileSystem.SYSTEM
        val path = "$dir/a.jpg"
        fs.createDirectories(dir.toPath())
        fs.write(path.toPath()) { writeUtf8("bytes") }
        store.enqueue(QueueItem("l1", "i1", "r1", "kitchen", path))
        val remote = FakeRemote()
        val pump = UploadPump(store, remote, fs) { }
        pump.drain()
        val done = store.load().single()
        assertEquals(QueueStatus.CONFIRMED, done.status)
        assertEquals("e1", done.evidenceId)
    }

    @Test
    fun retriesThenConfirms() = runTest {
        val (store, dir) = testStore()
        val fs = FileSystem.SYSTEM
        val path = "$dir/b.jpg"
        fs.createDirectories(dir.toPath())
        fs.write(path.toPath()) { writeUtf8("bytes") }
        store.enqueue(QueueItem("l1", "i1", null, "kitchen", path))
        val remote = FakeRemote(failures = 2)
        var sleeps = 0
        val pump = UploadPump(store, remote, fs) { sleeps++ }
        pump.drain()
        assertEquals(QueueStatus.CONFIRMED, store.load().single().status)
        assertEquals(3, remote.calls)
        assertEquals(2, sleeps)
    }
}
