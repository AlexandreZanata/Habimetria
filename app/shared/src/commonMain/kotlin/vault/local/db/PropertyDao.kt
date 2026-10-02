package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PropertyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(property: LocalProperty)

    @Update
    suspend fun update(property: LocalProperty)

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun get(id: String): LocalProperty?

    @Query("SELECT * FROM properties ORDER BY createdAtDevice DESC")
    suspend fun listAll(): List<LocalProperty>

    @Query("SELECT * FROM properties ORDER BY createdAtDevice DESC LIMIT :limit OFFSET :offset")
    suspend fun listPaged(limit: Int, offset: Int): List<LocalProperty>
}
