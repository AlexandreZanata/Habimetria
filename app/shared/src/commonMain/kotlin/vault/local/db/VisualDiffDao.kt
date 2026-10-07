package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface VisualDiffDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(diff: vault.local.db.VisualDiff): Long

    @Update
    suspend fun update(diff: vault.local.db.VisualDiff)

    @Query("SELECT * FROM visual_diffs WHERE exitRoomId = :exitRoomId AND shotCategory = :shotCategory")
    suspend fun get(exitRoomId: String, shotCategory: String): vault.local.db.VisualDiff?

    @Query("SELECT * FROM visual_diffs WHERE id = :id")
    suspend fun getById(id: String): vault.local.db.VisualDiff?

    @Query("SELECT * FROM visual_diffs WHERE exitInspectionId = :exitInspectionId ORDER BY exitRoomId ASC, shotCategory ASC")
    suspend fun listForExit(exitInspectionId: String): List<vault.local.db.VisualDiff>

    @Query("SELECT * FROM visual_diffs WHERE exitInspectionId = :exitInspectionId AND status = 'PENDING' ORDER BY exitRoomId ASC, shotCategory ASC")
    suspend fun listPending(exitInspectionId: String): List<vault.local.db.VisualDiff>
}
