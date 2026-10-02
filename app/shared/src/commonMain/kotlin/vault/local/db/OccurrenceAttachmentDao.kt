package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface OccurrenceAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: OccurrenceAttachment)

    @Query("SELECT * FROM occurrence_attachments WHERE occurrenceId = :occurrenceId")
    suspend fun listForOccurrence(occurrenceId: String): List<OccurrenceAttachment>
}
