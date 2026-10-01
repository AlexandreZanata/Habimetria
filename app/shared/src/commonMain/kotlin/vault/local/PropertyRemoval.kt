package vault.local

import vault.local.db.PropertyArchiveOutcome
import vault.local.db.PropertyDeleteOutcome
import vault.local.db.PropertyEligibility
import vault.local.db.PropertyRestoreOutcome
import vault.local.db.VaultDatabase

// U07 use cases: empty delete vs archive/restore for populated Properties.
// LOCAL_ONLY, no network. Signed Manifest bytes never include visibility,
// so archive/restore keeps hashes and signatures identical. UI confirms
// first (U08); this layer commits first, UI refreshes after.
class PropertyRemovalRepository(private val db: VaultDatabase) {
    suspend fun eligibility(propertyId: String): PropertyEligibility? =
        db.propertyRemovalDao().eligibilitySnapshot(propertyId)

    suspend fun deleteEmpty(propertyId: String): PropertyDeleteOutcome =
        db.propertyRemovalDao().deleteIfEmpty(propertyId)

    suspend fun archive(propertyId: String): PropertyArchiveOutcome =
        db.propertyRemovalDao().archive(propertyId)

    suspend fun restore(propertyId: String): PropertyRestoreOutcome =
        db.propertyRemovalDao().restore(propertyId)
}
