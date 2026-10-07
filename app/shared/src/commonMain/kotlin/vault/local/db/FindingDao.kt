package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface FindingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(finding: LocalFinding)

    @Query("SELECT * FROM findings WHERE roomId = :roomId")
    suspend fun listForRoom(roomId: String): List<LocalFinding>

    @Transaction
    @Query("SELECT * FROM findings WHERE evidenceId = :evidenceId")
    suspend fun listForEvidence(evidenceId: String): List<LocalFinding>

    @Query("DELETE FROM findings WHERE evidenceId = :evidenceId")
    suspend fun deleteForEvidence(evidenceId: String)
}
