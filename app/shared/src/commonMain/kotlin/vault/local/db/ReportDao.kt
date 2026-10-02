package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(report: LocalReport)

    @Update
    suspend fun update(report: LocalReport)

    @Query("SELECT * FROM reports WHERE manifestId = :manifestId ORDER BY versionNumber DESC, createdAtDevice DESC, id DESC LIMIT 1")
    suspend fun getByManifest(manifestId: String): LocalReport?

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun get(id: String): LocalReport?

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: String): Int
    @Query("SELECT * FROM reports WHERE manifestId = :manifestId ORDER BY versionNumber DESC, createdAtDevice DESC, id DESC")
    suspend fun listForManifest(manifestId: String): List<LocalReport>
}

