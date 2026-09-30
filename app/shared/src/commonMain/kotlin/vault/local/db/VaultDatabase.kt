package vault.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// Schema v5 (v4 + trusted issuers). Schema JSON exports to
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
        TrustedIssuer::class,
    ],
    version = 5,
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
    abstract fun trustDao(): TrustDao
}

@Suppress("KotlinNoActualForExpect")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}
