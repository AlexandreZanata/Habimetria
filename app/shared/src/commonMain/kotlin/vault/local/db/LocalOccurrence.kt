package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "occurrences",
    foreignKeys = [
        ForeignKey(
            entity = LocalProperty::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
        ),
    ],
    indices = [Index("propertyId")],
)
data class LocalOccurrence(
    @PrimaryKey val id: String,
    val propertyId: String,
    val title: String,
    val body: String?,
    val createdAtDevice: String,
)
