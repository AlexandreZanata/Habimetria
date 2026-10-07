package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ManifestDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(manifest: LocalManifest)

    @Query("SELECT * FROM manifests WHERE inspectionId = :inspectionId")
    suspend fun getByInspection(inspectionId: String): LocalManifest?

    @Query("SELECT * FROM manifests WHERE id = :id")
    suspend fun get(id: String): LocalManifest?

    @Query(
        "UPDATE inspections SET status = 'SEALED', sealedAtDevice = :sealedAt " +
            "WHERE id = :inspectionId AND status = 'DRAFT'",
    )
    suspend fun markSealed(inspectionId: String, sealedAt: String): Int

    // Atomic freeze: manifest row + terminal SEALED in one transaction.
    // Returns rows marked; 0 means a concurrent seal won the race.
    @Transaction
    suspend fun commitSeal(manifest: LocalManifest, inspectionId: String, sealedAt: String): Int {
        insert(manifest)
        return markSealed(inspectionId, sealedAt)
    }
}
