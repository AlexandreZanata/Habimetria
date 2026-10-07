package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_suggestions",
    indices = [
        Index("inspectionId"),
        Index(value = ["roomId", "evidenceId", "kind", "detail"], unique = true),
    ],
)
data class AiSuggestion(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomId: String,
    // "" when the hint is room-level (coverage gap).
    val evidenceId: String,
    // COVERAGE_GAP | POSSIBLE_STAIN | POSSIBLE_CRACK. Never liability.
    val kind: String,
    // Gap category name, "" for stain/crack hints.
    val detail: String,
    // PENDING | CONFIRMED | IGNORED.
    val status: String,
    val createdAtDevice: String,
)
