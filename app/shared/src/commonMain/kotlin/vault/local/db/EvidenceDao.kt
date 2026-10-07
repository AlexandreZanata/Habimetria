package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: LocalEvidence)

    @Query("SELECT * FROM evidences WHERE id = :id")
    suspend fun get(id: String): LocalEvidence?

    @Query("SELECT * FROM evidences WHERE inspectionId = :inspectionId ORDER BY sequence ASC")
    suspend fun listForInspection(inspectionId: String): List<LocalEvidence>

    @Query("SELECT * FROM evidences WHERE roomId = :roomId ORDER BY sequence ASC")
    suspend fun listForRoom(roomId: String): List<LocalEvidence>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM evidences WHERE inspectionId = :inspectionId")
    suspend fun maxSequence(inspectionId: String): Int

    @Query("DELETE FROM evidences WHERE id = :id")
    suspend fun delete(id: String)
    // Atomic sequence assignment: concurrent captures never share a number.
    @Transaction
    suspend fun insertSequenced(inspectionId: String, build: (Int) -> LocalEvidence): LocalEvidence {
        val row = build(maxSequence(inspectionId) + 1)
        insert(row)
        return row
    }
}
