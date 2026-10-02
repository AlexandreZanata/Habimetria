package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "occurrence_attachments",
    foreignKeys = [
        ForeignKey(
            entity = LocalOccurrence::class,
            parentColumns = ["id"],
            childColumns = ["occurrenceId"],
        ),
    ],
    indices = [Index("occurrenceId")],
)
data class OccurrenceAttachment(
    @PrimaryKey val id: String,
    val occurrenceId: String,
    val fileName: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val capturedAtDevice: String,
    val note: String?,
)

// Entry-vs-exit tags per room position. The pair references the latest
// photo of each side at tagging time; retakes don't rewrite history,
// they just become the new latest for the next tag.
