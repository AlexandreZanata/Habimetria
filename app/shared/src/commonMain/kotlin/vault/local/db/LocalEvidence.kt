package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidences",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
        ForeignKey(
            entity = LocalRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
        ),
    ],
    indices = [Index("inspectionId"), Index("roomId")],
)
data class LocalEvidence(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomId: String?,
    val fileName: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val sequence: Int,
    val capturedAtDevice: String,
    val origin: String,
    val shotCategory: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val locationStatus: String,
    // v2: shutter-time claim details. EXIF stays a separate device claim.
    val fixAgeMs: Long? = null,
    val provider: String? = null,
    val accuracyLevel: String? = null,
    val isMocked: Boolean = false,
    val exifLatitude: Double? = null,
    val exifLongitude: Double? = null,
    val mockSignal: String? = null,
    val fixTimeDevice: String? = null,
    val deviceUtcOffsetMinutes: Int? = null,
    val exifOrientation: Int? = null,
)
