package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
