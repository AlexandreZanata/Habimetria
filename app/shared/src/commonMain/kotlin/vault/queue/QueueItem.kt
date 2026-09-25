package vault.queue

import kotlinx.serialization.Serializable

// QueueItem is one captured photo awaiting server ack.
// States mirror docs/05-mobile/offline-upload.md.
@Serializable
data class QueueItem(
    val localId: String,
    val inspectionId: String,
    val roomId: String?,
    val roomKey: String,
    val filePath: String,
    val mime: String = "image/jpeg",
    val status: QueueStatus = QueueStatus.QUEUED,
    val attempts: Int = 0,
    val evidenceId: String? = null,
    val lastError: String? = null,
)

@Serializable
enum class QueueStatus {
    QUEUED,
    UPLOADING,
    CONFIRMED,
    FAILED,
}
