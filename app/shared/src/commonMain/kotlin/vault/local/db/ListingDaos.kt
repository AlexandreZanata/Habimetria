package vault.local.db

import androidx.room.Dao
import androidx.room.Query

// Bounded listing queries for the destination screens. Schema is
// unchanged (queries only, no migration): aggregation happens in
// LocalOverviewRepository, never per-row from the UI.
@Dao
interface InspectionListingDao {
    @Query("SELECT * FROM inspections ORDER BY createdAtDevice DESC LIMIT :limit OFFSET :offset")
    suspend fun listPaged(limit: Int, offset: Int): List<LocalInspection>

    @Query(
        "SELECT * FROM inspections WHERE status = :status " +
            "ORDER BY createdAtDevice DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun listPagedByStatus(status: String, limit: Int, offset: Int): List<LocalInspection>

    @Query("SELECT * FROM inspections WHERE id IN (:ids)")
    suspend fun listByIds(ids: List<String>): List<LocalInspection>
}

@Dao
interface TenancyListingDao {
    @Query("SELECT * FROM tenancies WHERE id IN (:ids)")
    suspend fun listByIds(ids: List<String>): List<LocalTenancy>
}

@Dao
interface PropertyListingDao {
    @Query("SELECT * FROM properties WHERE id IN (:ids)")
    suspend fun listByIds(ids: List<String>): List<LocalProperty>
}

@Dao
interface ManifestListingDao {
    @Query("SELECT * FROM manifests WHERE inspectionId IN (:inspectionIds)")
    suspend fun listForInspections(inspectionIds: List<String>): List<LocalManifest>
}

@Dao
interface ReportListingDao {
    @Query("SELECT * FROM reports ORDER BY createdAtDevice DESC LIMIT :limit OFFSET :offset")
    suspend fun listPaged(limit: Int, offset: Int): List<LocalReport>

    @Query("SELECT * FROM reports WHERE manifestId IN (:manifestIds)")
    suspend fun listForManifests(manifestIds: List<String>): List<LocalReport>

    @Query("SELECT * FROM reports WHERE inspectionId IN (:inspectionIds)")
    suspend fun listForInspections(inspectionIds: List<String>): List<LocalReport>
}
