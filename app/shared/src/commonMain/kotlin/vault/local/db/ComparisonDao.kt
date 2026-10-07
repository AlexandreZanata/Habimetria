package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ComparisonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comparison: Comparison)

    @Query("SELECT * FROM comparisons WHERE exitRoomId = :exitRoomId AND shotCategory = :shotCategory")
    suspend fun get(exitRoomId: String, shotCategory: String): Comparison?

    @Query("SELECT * FROM comparisons WHERE tenancyId = :tenancyId")
    suspend fun listForTenancy(tenancyId: String): List<Comparison>
}
