package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface FindingProblemDao {
    @Query("SELECT * FROM finding_problems ORDER BY builtin DESC, label ASC")
    suspend fun listAll(): List<LocalFindingProblem>

    @Query("SELECT * FROM finding_problems WHERE active = 1 ORDER BY builtin DESC, label ASC")
    suspend fun listActive(): List<LocalFindingProblem>

    @Query("SELECT * FROM finding_problems WHERE code = :code")
    suspend fun get(code: String): LocalFindingProblem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: LocalFindingProblem)

    @Query("UPDATE finding_problems SET active = :active WHERE code = :code")
    suspend fun setActive(code: String, active: Boolean): Int

    @Query("DELETE FROM finding_problems WHERE code = :code AND builtin = 0")
    suspend fun deleteCustom(code: String): Int
}
