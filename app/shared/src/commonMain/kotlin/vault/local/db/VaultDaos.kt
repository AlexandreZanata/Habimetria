package vault.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

// One DAO per aggregate root. Reads are suspend (no Flow yet);
// reactive streams arrive with the L2 UI shell if needed.
@Dao
interface PropertyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(property: LocalProperty)

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun get(id: String): LocalProperty?
}

@Dao
interface TenancyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tenancy: LocalTenancy)

    @Query("SELECT * FROM tenancies WHERE id = :id")
    suspend fun get(id: String): LocalTenancy?
}

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inspection: LocalInspection)

    @Update
    suspend fun update(inspection: LocalInspection)

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun get(id: String): LocalInspection?

    @Query("SELECT * FROM inspections WHERE status = 'DRAFT' ORDER BY createdAtDevice DESC")
    suspend fun listDrafts(): List<LocalInspection>

    @Query("SELECT * FROM inspections ORDER BY createdAtDevice DESC")
    suspend fun listAll(): List<LocalInspection>
}

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
}

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

    // Atomic sequence assignment: concurrent captures never share a number.
    @Transaction
    suspend fun insertSequenced(inspectionId: String, build: (Int) -> LocalEvidence): LocalEvidence {
        val row = build(maxSequence(inspectionId) + 1)
        insert(row)
        return row
    }
}

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

@Dao
interface FindingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(finding: LocalFinding)

    @Query("SELECT * FROM findings WHERE roomId = :roomId")
    suspend fun listForRoom(roomId: String): List<LocalFinding>

    @Transaction
    @Query("SELECT * FROM findings WHERE evidenceId = :evidenceId")
    suspend fun listForEvidence(evidenceId: String): List<LocalFinding>
}
