package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TrustDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issuer: TrustedIssuer)

    @Query("SELECT * FROM trusted_issuers WHERE keyId = :keyId")
    suspend fun get(keyId: String): TrustedIssuer?

    @Query("SELECT * FROM trusted_issuers ORDER BY addedAtDevice DESC")
    suspend fun list(): List<TrustedIssuer>

    @Query("DELETE FROM trusted_issuers WHERE keyId = :keyId")
    suspend fun delete(keyId: String)
}
