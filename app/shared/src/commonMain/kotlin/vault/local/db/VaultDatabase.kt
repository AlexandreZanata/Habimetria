package vault.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// Schema v2 (v1 + location/EXIF columns on evidences). Manifest/Report
// tables land with L5. Schema JSON exports to shared/schemas; every
// version bump ships an explicit Migration — never destructive.
@Database(
    entities = [
        LocalProperty::class,
        LocalTenancy::class,
        LocalInspection::class,
        LocalRoom::class,
        LocalEvidence::class,
        LocalFinding::class,
    ],
    version = 2,
)
@ConstructedBy(VaultDatabaseConstructor::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun tenancyDao(): TenancyDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun roomDao(): RoomDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun findingDao(): FindingDao
}

@Suppress("KotlinNoActualForExpect")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}
