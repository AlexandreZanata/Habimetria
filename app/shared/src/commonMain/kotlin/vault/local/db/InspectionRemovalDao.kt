package vault.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import vault.local.STATUS_SEALED

// Inspection deletion: DRAFT and SEALED both removable with explicit
// confirmation. Rows delete in FK order inside one transaction; files
// (originals, derivatives, PDF, receipt) clean up after the commit and
// never gate it — startup reconcile sweeps orphans. Comparisons and
// visual diffs pin both sides' rooms, so an inspection referenced by
// them is blocked instead of corrupting the other side's review state.
enum class InspectionBlockReason {
    HAS_COMPARISONS,
}

sealed interface InspectionDeleteOutcome {
    data object Deleted : InspectionDeleteOutcome
    data object Missing : InspectionDeleteOutcome
    data class Blocked(val reason: InspectionBlockReason) : InspectionDeleteOutcome
}

sealed interface InspectionRemovalPlan {
    data object Missing : InspectionRemovalPlan
    data class Blocked(val reason: InspectionBlockReason) : InspectionRemovalPlan
    data class Ready(
        val sealed: Boolean,
        val evidenceIds: List<String>,
        val reportId: String?,
    ) : InspectionRemovalPlan
}

@Dao
interface InspectionRemovalDao {
    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getInspection(id: String): LocalInspection?

    @Query("SELECT id FROM rooms WHERE inspectionId = :inspectionId")
    suspend fun roomIds(inspectionId: String): List<String>

    @Query("SELECT id FROM evidences WHERE inspectionId = :inspectionId")
    suspend fun evidenceIds(inspectionId: String): List<String>

    @Query("SELECT * FROM manifests WHERE inspectionId = :inspectionId")
    suspend fun manifest(inspectionId: String): LocalManifest?

    @Query("SELECT * FROM reports WHERE manifestId = :manifestId")
    suspend fun report(manifestId: String): LocalReport?

    @Query("SELECT COUNT(*) FROM comparisons WHERE entryRoomId IN (:roomIds) OR exitRoomId IN (:roomIds)")
    suspend fun comparisonCount(roomIds: List<String>): Int

    @Query("SELECT COUNT(*) FROM visual_diffs WHERE entryRoomId IN (:roomIds) OR exitRoomId IN (:roomIds)")
    suspend fun visualDiffCount(roomIds: List<String>): Int

    @Query("DELETE FROM findings WHERE roomId IN (SELECT id FROM rooms WHERE inspectionId = :inspectionId)")
    suspend fun deleteFindings(inspectionId: String)

    @Query("DELETE FROM evidences WHERE inspectionId = :inspectionId")
    suspend fun deleteEvidences(inspectionId: String)

    @Query("DELETE FROM ai_suggestions WHERE inspectionId = :inspectionId")
    suspend fun deleteSuggestions(inspectionId: String)

    @Query("DELETE FROM rooms WHERE inspectionId = :inspectionId")
    suspend fun deleteRooms(inspectionId: String)

    @Query("DELETE FROM reports WHERE manifestId = :manifestId")
    suspend fun deleteReports(manifestId: String)

    @Query("DELETE FROM manifests WHERE inspectionId = :inspectionId")
    suspend fun deleteManifest(inspectionId: String)

    @Query("DELETE FROM inspections WHERE id = :inspectionId")
    suspend fun deleteInspection(inspectionId: String): Int

    private suspend fun linkedReviewCount(inspectionId: String): Int {
        val rooms = roomIds(inspectionId)
        if (rooms.isEmpty()) return 0
        return comparisonCount(rooms) + visualDiffCount(rooms)
    }

    @Transaction
    suspend fun planFor(inspectionId: String): InspectionRemovalPlan {
        val inspection = getInspection(inspectionId) ?: return InspectionRemovalPlan.Missing
        if (linkedReviewCount(inspectionId) > 0) {
            return InspectionRemovalPlan.Blocked(InspectionBlockReason.HAS_COMPARISONS)
        }
        val manifest = manifest(inspectionId)
        return InspectionRemovalPlan.Ready(
            sealed = inspection.status == STATUS_SEALED,
            evidenceIds = evidenceIds(inspectionId),
            reportId = manifest?.let { report(it.id)?.id },
        )
    }

    // Eligibility rechecks inside the same transaction as the deletes,
    // so a stale dialog snapshot cannot authorize after a comparison
    // or visual diff appears.
    @Transaction
    suspend fun commitRemoval(inspectionId: String): InspectionDeleteOutcome {
        if (getInspection(inspectionId) == null) return InspectionDeleteOutcome.Missing
        if (linkedReviewCount(inspectionId) > 0) {
            return InspectionDeleteOutcome.Blocked(InspectionBlockReason.HAS_COMPARISONS)
        }
        val manifest = manifest(inspectionId)
        if (manifest != null) deleteReports(manifest.id)
        deleteFindings(inspectionId)
        deleteEvidences(inspectionId)
        deleteSuggestions(inspectionId)
        deleteRooms(inspectionId)
        if (manifest != null) deleteManifest(inspectionId)
        deleteInspection(inspectionId)
        return InspectionDeleteOutcome.Deleted
    }
}
