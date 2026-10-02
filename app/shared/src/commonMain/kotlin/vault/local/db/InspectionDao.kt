package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inspection: LocalInspection)

    @Update
    suspend fun update(inspection: LocalInspection)

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun get(id: String): LocalInspection?

    @Query("SELECT * FROM inspections WHERE status = 'DRAFT' ORDER BY createdAtDevice DESC")
    suspend fun listDrafts(): List<LocalInspection>

    @Query("SELECT * FROM inspections WHERE tenancyId = :tenancyId ORDER BY createdAtDevice ASC")
    suspend fun listForTenancy(tenancyId: String): List<LocalInspection>

    @Query("SELECT * FROM inspections ORDER BY createdAtDevice DESC")
    suspend fun listAll(): List<LocalInspection>

    @Query(
        "SELECT inspections.* FROM inspections " +
            "LEFT JOIN tenancies ON tenancies.id = inspections.tenancyId " +
            "WHERE COALESCE(inspections.propertyId, tenancies.propertyId) = :propertyId " +
            "ORDER BY inspections.createdAtDevice DESC LIMIT :limit",
    )
    suspend fun listForProperty(propertyId: String, limit: Int): List<LocalInspection>

    @Query("SELECT * FROM inspections WHERE tenancyId IN (:tenancyIds) ORDER BY createdAtDevice DESC")
    suspend fun listForTenancies(tenancyIds: List<String>): List<LocalInspection>
    @Query("SELECT inspections.* FROM inspections LEFT JOIN tenancies ON tenancies.id = inspections.tenancyId WHERE COALESCE(inspections.propertyId, tenancies.propertyId) IN (:propertyIds) ORDER BY inspections.createdAtDevice DESC")
    suspend fun listForProperties(propertyIds: List<String>): List<LocalInspection>
}


