package vault.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index(value = ["manifestId"]), Index("inspectionId")],
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
    @ColumnInfo(defaultValue = "1") val versionNumber: Int = 1,
)

// Post-seal changes live here, never in the sealed inspection.
// Occurrences belong to the Property, so they survive across
// inspections and never reopen a terminal SEALED state.
