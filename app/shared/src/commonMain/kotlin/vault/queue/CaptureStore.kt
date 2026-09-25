package vault.queue

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

// CaptureStore persists the upload queue as one JSON file, written
// atomically (tmp + move). Files live next to it. No DB in M2:
// ≤300 small rows, rewritten per transition. Room replaces this if
// the queue outgrows atomic rewrites.
class CaptureStore(
    private val fs: FileSystem,
    private val dir: String,
    private val json: Json = Json,
) {
    private val file = "$dir/queue.json".toPath()

    fun load(): List<QueueItem> {
        if (!fs.exists(file)) return emptyList()
        return try {
            json.decodeFromString<List<QueueItem>>(fs.read(file) { readUtf8() })
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(items: List<QueueItem>) {
        fs.createDirectories("$dir".toPath())
        val tmp = "$dir/queue.json.tmp".toPath()
        fs.write(tmp) { writeUtf8(json.encodeToString(items)) }
        fs.atomicMove(tmp, file)
    }

    fun enqueue(item: QueueItem): List<QueueItem> = (load() + item).also { save(it) }

    fun update(localId: String, change: (QueueItem) -> QueueItem): List<QueueItem> =
        load().map { if (it.localId == localId) change(it) else it }.also { save(it) }

    fun pending(): List<QueueItem> =
        load().filter { it.status == QueueStatus.QUEUED || it.status == QueueStatus.FAILED }
}
