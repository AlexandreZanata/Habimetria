package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoomPresetDao {
    @Query("SELECT * FROM room_presets ORDER BY sort, label")
    suspend fun listAll(): List<LocalRoomPreset>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: LocalRoomPreset)

    @Query("UPDATE room_presets SET label = :label WHERE id = :id")
    suspend fun rename(id: String, label: String): Int

    @Query("DELETE FROM room_presets WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT COALESCE(MAX(sort), -1) FROM room_presets")
    suspend fun maxSort(): Int
}
