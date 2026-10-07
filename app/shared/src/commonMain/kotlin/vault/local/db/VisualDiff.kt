package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visual_diffs",
    indices = [
        Index("exitInspectionId"),
        Index(value = ["exitRoomId", "shotCategory"], unique = true),
    ],
)
data class VisualDiff(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val exitInspectionId: String,
    val entryRoomId: String,
    val exitRoomId: String,
    val shotCategory: String,
    // "" when that side has no photo yet.
    val entryEvidenceId: String,
    val exitEvidenceId: String,
    // Comma-separated DiffSignal serials, strongest first.
    val signals: String,
    // PENDING | REVIEWED | DISMISSED.
    val status: String,
    val createdAtDevice: String,
)

// Trust roots the user explicitly accepted: pinned fingerprints or
// publisher-certified installers. Unknown by default; nothing here is
// ever provisioned silently by imported content.
// V1.4 inspector hints. Suggestions never decide: the user confirms
// (creates a Finding) or ignores. Sealed inspections freeze them.
