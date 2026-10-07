package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface AiSuggestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(suggestion: vault.local.db.AiSuggestion): Long

    @Update
    suspend fun update(suggestion: vault.local.db.AiSuggestion)

    @Query("SELECT * FROM ai_suggestions WHERE id = :id")
    suspend fun get(id: String): vault.local.db.AiSuggestion?

    @Query("SELECT * FROM ai_suggestions WHERE inspectionId = :inspectionId ORDER BY roomId ASC, evidenceId ASC, kind ASC")
    suspend fun listForInspection(inspectionId: String): List<vault.local.db.AiSuggestion>

    @Query("SELECT * FROM ai_suggestions WHERE inspectionId = :inspectionId AND status = 'PENDING' ORDER BY roomId ASC, evidenceId ASC, kind ASC")
    suspend fun listPending(inspectionId: String): List<vault.local.db.AiSuggestion>
}
