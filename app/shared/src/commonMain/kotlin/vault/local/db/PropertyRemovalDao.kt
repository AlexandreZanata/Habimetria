package vault.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

// U07 removal: eligibility counts plus transactional delete/archive/restore.
// The eligibility recheck lives inside the same transaction as the write,
// so a stale UI count cannot authorize removal after a Tenancy, Occurrence
// or mirror reference appears. Storage failures propagate, never become
// empty results. No file cleanup here: empty deletion never removes files
// and archive never touches originals/derivatives.
enum class PropertyBlockReason {
    HAS_TENANCIES,
    HAS_INSPECTIONS,
    HAS_OCCURRENCES,
    HAS_MIRROR_LINK,
}

data class PropertyEligibility(
    val propertyId: String,
    val archived: Boolean,
    val tenancyCount: Int,
    val occurrenceCount: Int,
    val hasMirrorLink: Boolean,
    val inspectionCount: Int = 0,
) {
    val canDelete: Boolean =
        tenancyCount == 0 && inspectionCount == 0 && occurrenceCount == 0 && !hasMirrorLink
}

sealed interface PropertyDeleteOutcome {
    data object Deleted : PropertyDeleteOutcome
    data object Missing : PropertyDeleteOutcome
    data class Blocked(val reason: PropertyBlockReason) : PropertyDeleteOutcome
}

sealed interface PropertyArchiveOutcome {
    data object Archived : PropertyArchiveOutcome
    data object AlreadyArchived : PropertyArchiveOutcome
    data object Missing : PropertyArchiveOutcome
}

sealed interface PropertyRestoreOutcome {
    data object Restored : PropertyRestoreOutcome
    data object AlreadyActive : PropertyRestoreOutcome
    data object Missing : PropertyRestoreOutcome
}

@Dao
interface PropertyRemovalDao {
    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun get(id: String): LocalProperty?

    @Query("SELECT COUNT(*) FROM tenancies WHERE propertyId = :propertyId")
    suspend fun countTenancies(propertyId: String): Int

    @Query("SELECT COUNT(*) FROM inspections WHERE propertyId = :propertyId")
    suspend fun countInspections(propertyId: String): Int

    @Query("SELECT COUNT(*) FROM occurrences WHERE propertyId = :propertyId")
    suspend fun countOccurrences(propertyId: String): Int

    @Query("SELECT COUNT(*) FROM mirror_links WHERE localId = :propertyId")
    suspend fun countMirrorLinks(propertyId: String): Int

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE properties SET archived = 1 WHERE id = :id AND archived = 0")
    suspend fun markArchived(id: String): Int

    @Query("UPDATE properties SET archived = 0 WHERE id = :id AND archived = 1")
    suspend fun markRestored(id: String): Int

    @Transaction
    suspend fun eligibilitySnapshot(propertyId: String): PropertyEligibility? {
        val current = get(propertyId) ?: return null
        return PropertyEligibility(
            propertyId = propertyId,
            archived = current.archived,
            tenancyCount = countTenancies(propertyId),
            occurrenceCount = countOccurrences(propertyId),
            hasMirrorLink = countMirrorLinks(propertyId) > 0,
            inspectionCount = countInspections(propertyId),
        )
    }

    // Empty unlinked only. Missing covers double taps after a first delete.
    // Blocked preserves every related row; FKs stay NO ACTION, never cascade.
    @Transaction
    suspend fun deleteIfEmpty(propertyId: String): PropertyDeleteOutcome {
        if (get(propertyId) == null) return PropertyDeleteOutcome.Missing
        if (countTenancies(propertyId) > 0) {
            return PropertyDeleteOutcome.Blocked(PropertyBlockReason.HAS_TENANCIES)
        }
        if (countInspections(propertyId) > 0) return PropertyDeleteOutcome.Blocked(PropertyBlockReason.HAS_INSPECTIONS)
        if (countOccurrences(propertyId) > 0) {
            return PropertyDeleteOutcome.Blocked(PropertyBlockReason.HAS_OCCURRENCES)
        }
        if (countMirrorLinks(propertyId) > 0) {
            return PropertyDeleteOutcome.Blocked(PropertyBlockReason.HAS_MIRROR_LINK)
        }
        deleteById(propertyId)
        return PropertyDeleteOutcome.Deleted
    }

    @Transaction
    suspend fun archive(propertyId: String): PropertyArchiveOutcome {
        val current = get(propertyId) ?: return PropertyArchiveOutcome.Missing
        if (current.archived) return PropertyArchiveOutcome.AlreadyArchived
        markArchived(propertyId)
        return PropertyArchiveOutcome.Archived
    }

    @Transaction
    suspend fun restore(propertyId: String): PropertyRestoreOutcome {
        val current = get(propertyId) ?: return PropertyRestoreOutcome.Missing
        if (!current.archived) return PropertyRestoreOutcome.AlreadyActive
        markRestored(propertyId)
        return PropertyRestoreOutcome.Restored
    }
}
