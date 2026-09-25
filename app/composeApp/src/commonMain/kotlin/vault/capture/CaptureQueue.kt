package vault.capture

// Offline-first queue item. Server ack (evidence_id + received_at_server)
// flips status to CONFIRMED. Never show saved before ack.
enum class UploadStatus { QUEUED, UPLOADING, CONFIRMED, FAILED }

data class CaptureItem(
    val localId: String,
    val roomKey: String,
    val filePath: String,
    val idempotencyKey: String,
    val status: UploadStatus = UploadStatus.QUEUED,
    val serverEvidenceId: String? = null
)
