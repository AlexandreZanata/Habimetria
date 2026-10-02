package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class LocalProperty(
    @PrimaryKey val id: String,
    val address: String,
    // Structured parts (v12): checkout-style input. `address` stays the
    // formatted display line; legacy rows keep street backfilled from it.
    val street: String = "",
    val number: String = "",
    val complement: String? = null,
    val neighborhood: String = "",
    // Friendly name for the property picker ("Casa da praia").
    val nickname: String = "",
    // Local visibility for U07 removal: false = active, true = archived.
    // Organizational only, never part of frozen Manifest payloads.
    val archived: Boolean = false,
    val createdAtDevice: String,
)
