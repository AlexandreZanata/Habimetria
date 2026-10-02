package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface OccurrenceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(occurrence: LocalOccurrence)

    @Query("SELECT * FROM occurrences WHERE id = :id")
    suspend fun get(id: String): LocalOccurrence?

    @Query("SELECT * FROM occurrences WHERE propertyId = :propertyId ORDER BY createdAtDevice DESC")
    suspend fun listForProperty(propertyId: String): List<LocalOccurrence>
}
