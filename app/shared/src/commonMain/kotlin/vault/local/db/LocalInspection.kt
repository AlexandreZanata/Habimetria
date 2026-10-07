package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = LocalTenancy::class,
            parentColumns = ["id"],
            childColumns = ["tenancyId"],
        ),
        ForeignKey(entity = LocalProperty::class, parentColumns = ["id"], childColumns = ["propertyId"]),
    ],
    indices = [Index("tenancyId"), Index("propertyId")],
)
data class LocalInspection(
    @PrimaryKey val id: String,
    val tenancyId: String?,
    val kind: String,
    val status: String,
    val createdAtDevice: String,
    val sealedAtDevice: String?,
    val propertyId: String? = null,
)
