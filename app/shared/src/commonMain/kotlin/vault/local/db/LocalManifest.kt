package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
