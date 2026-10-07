package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comparisons",
    indices = [Index(value = ["exitRoomId", "shotCategory"], unique = true)],
)
data class Comparison(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val entryRoomId: String,
    val exitRoomId: String,
    val shotCategory: String,
    val entryEvidenceId: String?,
    val exitEvidenceId: String?,
    // no_change | changed | wear. Never liability, only visual state.
    val tag: String,
    val createdAtDevice: String,
)

// V1.5 visual-difference list. One row per exit position where an
// offline heuristic fired (presence, findings, user tag or location
// change). Byte difference alone never raises a row: two captures
// always differ at byte level. Review state only; never liability.
