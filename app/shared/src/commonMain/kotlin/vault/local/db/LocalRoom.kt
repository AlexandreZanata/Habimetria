package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
    ],
    indices = [Index("inspectionId")],
)
data class LocalRoom(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomKey: String,
    val label: String,
    val sort: Int,
    val naCategories: String,
    // Exit inspections mirror entry rooms: same labels/positions, new
    // ids, linked here for V1.3 side-by-side pairing.
    val sourceRoomId: String? = null,
)
