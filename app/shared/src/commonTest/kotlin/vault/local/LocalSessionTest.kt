package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.queue.CaptureStore
import vault.queue.QueueItem
import vault.queue.QueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private var sessionSeq = 0

private fun tempDir(prefix: String): String {
    sessionSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("$prefix-$sessionSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir
}

class LocalSessionTest {

    @Test
    fun freshStartupNeedsNoNetworkAndFindsNothing() = runTest {
        val session = openLocalSession(tempDir("vault-sess"), SyncMode.LOCAL_ONLY)
        assertFalse(session.uploadsAllowed)
        val startup = session.startup()
        assertNull(startup.migration)
        assertEquals(0, startup.reconcile.checked)
        assertFalse(startup.reconcile.hasProblems())
        session.close()
    }

    @Test
    fun startupMigratesLegacyQueueOffline() = runTest {
        val dir = tempDir("vault-sess-mig")
        val photo = "$dir/photo.jpg"
        FileSystem.SYSTEM.write(photo.toPath()) { writeUtf8("bytes") }
        CaptureStore(FileSystem.SYSTEM, "$dir/queue")
            .enqueue(QueueItem("q1", "server-ins", "server-room", "Sala", photo))

        val session = openLocalSession(dir, SyncMode.LOCAL_ONLY)
        val startup = session.startup()
        assertEquals(1, startup.migration?.imported)
        assertEquals(1, session.repository.evidencesFor(startup.migration!!.inspectionId).size)
        assertEquals(1, startup.reconcile.checked)
        assertFalse(startup.reconcile.hasProblems())
        session.close()
    }

    @Test
    fun navigationProgressSurvivesRestart() {
        val dir = tempDir("vault-nav")
        val store = NavigationStore(FileSystem.SYSTEM, dir)
        assertEquals(NavigationProgress(), store.load())
        store.save(NavigationProgress("ins1", "room2"))
        // New instance, same dir: resume where the user stopped.
        assertEquals(NavigationProgress("ins1", "room2"), NavigationStore(FileSystem.SYSTEM, dir).load())
        store.clear()
        assertEquals(NavigationProgress(), store.load())
    }

    @Test
    fun workerGateAndIndicatorMapping() {
        assertFalse(uploadsEnabled(SyncMode.LOCAL_ONLY))
        assertTrue(uploadsEnabled(SyncMode.WITH_SERVER_SYNC))
        assertEquals(SyncIndicator.QUEUED, QueueStatus.QUEUED.toSyncIndicator())
        assertEquals(SyncIndicator.CONFIRMED, QueueStatus.CONFIRMED.toSyncIndicator())
        // Local durability never implies server confirmation.
        val local = StorageStatus(storedLocally = true, sync = SyncIndicator.LOCAL_ONLY)
        assertTrue(local.storedLocally)
    }
}
