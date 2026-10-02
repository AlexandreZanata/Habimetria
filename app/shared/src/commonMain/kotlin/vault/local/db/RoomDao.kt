package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(room: LocalRoom)

    @Update
    suspend fun update(room: LocalRoom)

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun get(id: String): LocalRoom?

    @Query("SELECT * FROM rooms WHERE inspectionId = :inspectionId ORDER BY sort ASC")
    suspend fun listForInspection(inspectionId: String): List<LocalRoom>

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT inspectionId AS inspectionId, COUNT(*) AS count FROM rooms " +
            "WHERE inspectionId IN (:inspectionIds) GROUP BY inspectionId",
    )
    suspend fun countsForInspections(inspectionIds: List<String>): List<RoomCount>
}
