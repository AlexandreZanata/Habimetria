package vault.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// Schema v7 (v6 + room source links for exit pairing). Schema JSON
// exports to shared/schemas; every bump ships an explicit Migration.
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
        LocalOccurrence::class,
        OccurrenceAttachment::class,
    ],
    version = 7,
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
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun occurrenceAttachmentDao(): OccurrenceAttachmentDao
}

@Suppress("KotlinNoActualForExpect")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}
