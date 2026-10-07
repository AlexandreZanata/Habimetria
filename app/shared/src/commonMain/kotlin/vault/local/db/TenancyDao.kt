package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TenancyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tenancy: LocalTenancy)

    @Query("SELECT * FROM tenancies WHERE id = :id")
    suspend fun get(id: String): LocalTenancy?

    @Query("SELECT * FROM tenancies WHERE propertyId = :propertyId ORDER BY createdAtDevice DESC")
    suspend fun listForProperty(propertyId: String): List<LocalTenancy>

    @Query("SELECT * FROM tenancies WHERE propertyId IN (:propertyIds)")
    suspend fun listForProperties(propertyIds: List<String>): List<LocalTenancy>
}
