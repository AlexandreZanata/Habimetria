package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_issuers")
data class TrustedIssuer(
    @PrimaryKey val keyId: String,
    val label: String,
    // PINNED (compared fingerprint) or CERTIFIED (publisher certificate).
    val source: String,
    val addedAtDevice: String,
)
