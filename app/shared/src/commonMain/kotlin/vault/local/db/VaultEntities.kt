package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Local schema v1: capture core. Manifest/Report tables arrive with L5.
// Device time and GPS stay device claims; no server-certified columns here.
@Entity(tableName = "properties")
data class LocalProperty(
    @PrimaryKey val id: String,
    val address: String,
    val createdAtDevice: String,
)

@Entity(
    tableName = "tenancies",
    foreignKeys = [
        ForeignKey(
            entity = LocalProperty::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
        ),
    ],
    indices = [Index("propertyId")],
)
data class LocalTenancy(
    @PrimaryKey val id: String,
    val propertyId: String,
    val type: String,
    val createdAtDevice: String,
)

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = LocalTenancy::class,
            parentColumns = ["id"],
            childColumns = ["tenancyId"],
        ),
    ],
    indices = [Index("tenancyId")],
)
data class LocalInspection(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val kind: String,
    val status: String,
    val createdAtDevice: String,
    val sealedAtDevice: String?,
)

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
)

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
)

@Entity(
    tableName = "manifests",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
    ],
    indices = [Index(value = ["inspectionId"], unique = true)],
)
data class LocalManifest(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val formatVersion: Int,
    val canonicalSha256: String,
    val canonicalJson: String,
    val sealedAtDevice: String,
    val signerKeyId: String,
    val algorithm: String,
    val hardwareBacked: Boolean,
    val signature: String,
)

@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
        ForeignKey(
            entity = LocalManifest::class,
            parentColumns = ["id"],
            childColumns = ["manifestId"],
        ),
    ],
    indices = [Index(value = ["manifestId"], unique = true)],
)
data class LocalReport(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val manifestId: String,
    val manifestSha256: String,
    // RENDERING | READY | FAILED. Retries update the same row;
    // SEALED never reopens.
    val status: String,
    val pdfSha256: String?,
    val pdfFileName: String?,
    val receiptSignature: String?,
    val signerKeyId: String,
    val algorithm: String,
    val hardwareBacked: Boolean,
    val createdAtDevice: String,
    val completedAtDevice: String?,
)

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = LocalRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
        ),
        ForeignKey(
            entity = LocalEvidence::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
        ),
    ],
    indices = [Index("roomId"), Index("evidenceId")],
)
data class LocalFinding(
    @PrimaryKey val id: String,
    val roomId: String,
    val evidenceId: String,
    val preset: String,
    val note: String?,
)
