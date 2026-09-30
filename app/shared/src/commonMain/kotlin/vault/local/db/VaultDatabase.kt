package vault.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// Schema v4 (v3 + reports + detached receipts). Schema JSON exports to
// shared/schemas; every version bump ships an explicit Migration.
@Database(
    entities = [
        LocalProperty::class,
        LocalTenancy::class,
        LocalInspection::class,
        LocalRoom::class,
        LocalEvidence::class,
        LocalFinding::class,
        LocalManifest::class,
        LocalReport::class,
    ],
    version = 4,
)
@ConstructedBy(VaultDatabaseConstructor::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun tenancyDao(): TenancyDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun roomDao(): RoomDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun findingDao(): FindingDao
    abstract fun manifestDao(): ManifestDao
    abstract fun reportDao(): ReportDao
}

@Suppress("KotlinNoActualForExpect")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}
