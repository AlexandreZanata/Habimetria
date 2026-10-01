package vault.local

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import vault.queue.CaptureStore
import vault.queue.QueueItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private var migrationSeq = 0

class QueueMigrationTest {

    @Test
    fun migratesPhotosWithoutLossAndClearsQueue() = runTest {
        migrationSeq++
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-mig-$migrationSeq").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())

        // Legacy queue with two real photos and one dangling reference.
        val queue = CaptureStore(fs, "$dir/queue")
        for (i in 1..2) {
            val photo = "$dir/photo-$i.jpg"
            fs.write(photo.toPath()) { writeUtf8("photo-$i-bytes-ç") }
            queue.enqueue(QueueItem("q$i", "server-ins", "server-room", "Cozinha", photo))
        }
        queue.enqueue(QueueItem("q3", "server-ins", "server-room", "Cozinha", "$dir/gone.jpg"))

        val db = localDatabaseBuilder("$dir/vault.db").build()
        val originals = OriginalStore(fs, dir)
        val report = QueueMigration(fs, dir).migrate(queue, db, originals)

        assertEquals(2, report.imported)
        assertEquals(listOf("q3"), report.missingFiles)
        // Photos land intact: same bytes, grouped under the legacy label.
        val evidences = db.evidenceDao().listForInspection(report.inspectionId)
        assertEquals(2, evidences.size)
        assertEquals(listOf(1, 2), evidences.map { it.sequence })
        for (evidence in evidences) {
            val bytes = originals.readOriginal(evidence.id)
            assertEquals(originals.sha256Of(bytes), evidence.sha256)
        }
        val rooms = db.roomDao().listForInspection(report.inspectionId)
        assertEquals(listOf("Cozinha"), rooms.map { it.label })
        // Queue cleared and marker written: a restart cannot duplicate.
        assertTrue(queue.load().isEmpty())
        val second = QueueMigration(fs, dir).migrate(queue, db, originals)
        assertTrue(second.skippedAlreadyMigrated)
        assertEquals(report.inspectionId, second.inspectionId)
        assertEquals(2, db.evidenceDao().listForInspection(report.inspectionId).size)
        db.close()
    }
}
