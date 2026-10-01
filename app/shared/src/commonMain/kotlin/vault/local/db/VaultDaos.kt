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

    @Query("SELECT * FROM inspections WHERE tenancyId = :tenancyId ORDER BY createdAtDevice ASC")
    suspend fun listForTenancy(tenancyId: String): List<LocalInspection>

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
interface OccurrenceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(occurrence: LocalOccurrence)

    @Query("SELECT * FROM occurrences WHERE id = :id")
    suspend fun get(id: String): LocalOccurrence?

    @Query("SELECT * FROM occurrences WHERE propertyId = :propertyId ORDER BY createdAtDevice DESC")
    suspend fun listForProperty(propertyId: String): List<LocalOccurrence>
}

@Dao
interface OccurrenceAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: OccurrenceAttachment)

    @Query("SELECT * FROM occurrence_attachments WHERE occurrenceId = :occurrenceId")
    suspend fun listForOccurrence(occurrenceId: String): List<OccurrenceAttachment>
}

@Dao
interface ComparisonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comparison: Comparison)

    @Query("SELECT * FROM comparisons WHERE exitRoomId = :exitRoomId AND shotCategory = :shotCategory")
    suspend fun get(exitRoomId: String, shotCategory: String): Comparison?

    @Query("SELECT * FROM comparisons WHERE tenancyId = :tenancyId")
    suspend fun listForTenancy(tenancyId: String): List<Comparison>
}

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

@Dao
interface TrustDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issuer: TrustedIssuer)

    @Query("SELECT * FROM trusted_issuers WHERE keyId = :keyId")
    suspend fun get(keyId: String): TrustedIssuer?

    @Query("SELECT * FROM trusted_issuers ORDER BY addedAtDevice DESC")
    suspend fun list(): List<TrustedIssuer>

    @Query("DELETE FROM trusted_issuers WHERE keyId = :keyId")
    suspend fun delete(keyId: String)
}

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(report: LocalReport)

    @Update
    suspend fun update(report: LocalReport)

    @Query("SELECT * FROM reports WHERE manifestId = :manifestId")
    suspend fun getByManifest(manifestId: String): LocalReport?

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun get(id: String): LocalReport?
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

    @Query("DELETE FROM findings WHERE evidenceId = :evidenceId")
    suspend fun deleteForEvidence(evidenceId: String)
}
