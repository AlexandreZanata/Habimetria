package vault.queue

import kotlinx.coroutines.delay
import okio.FileSystem
import okio.Path.Companion.toPath

// UploadPump drains one queue item per call with backoff.
// Foreground UI and WorkManager share it. At-least-once: the server
// has no idempotency key yet, so a timeout after a successful store
// can duplicate — the ack path (evidence_id) makes dups visible.
class UploadPump(
    private val store: CaptureStore,
    private val remote: EvidenceRemote,
    private val fs: FileSystem,
    private val sleeper: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun processNext(): Boolean {
        val item = store.pending().firstOrNull() ?: return false
        if (item.attempts >= Backoff.MAX_ATTEMPTS) {
            store.update(item.localId) { it.copy(status = QueueStatus.FAILED) }
            return true
        }
        store.update(item.localId) {
            it.copy(status = QueueStatus.UPLOADING, attempts = it.attempts + 1, lastError = null)
        }
        try {
            val bytes = fs.read(item.filePath.toPath()) { readByteArray() }
            val ack = remote.uploadPhoto(item.inspectionId, item.roomId, bytes, item.mime)
            store.update(item.localId) {
                it.copy(status = QueueStatus.CONFIRMED, evidenceId = ack.id)
            }
        } catch (e: Exception) {
            store.update(item.localId) {
                it.copy(status = QueueStatus.QUEUED, lastError = e.message?.take(200))
            }
            sleeper(Backoff.delayMs(item.attempts))
        }
        return true
    }

    suspend fun drain() {
        while (processNext()) { /* until empty */ }
    }
}
