package vault.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// Schema v15 (v14 + finding_problems for problem customization).
// Schema JSON exports to shared/schemas; every bump ships an explicit Migration.
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
        Comparison::class,
        AiSuggestion::class,
        VisualDiff::class,
        MirrorLink::class,
        LocalFindingProblem::class,
    ],
    version = 15,
)
@ConstructedBy(VaultDatabaseConstructor::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun tenancyDao(): TenancyDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun roomDao(): RoomDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun findingDao(): FindingDao
    abstract fun findingProblemDao(): FindingProblemDao
    abstract fun manifestDao(): ManifestDao
    abstract fun reportDao(): ReportDao
    abstract fun trustDao(): TrustDao
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun occurrenceAttachmentDao(): OccurrenceAttachmentDao
    abstract fun comparisonDao(): ComparisonDao
    abstract fun aiSuggestionDao(): AiSuggestionDao
    abstract fun visualDiffDao(): VisualDiffDao
    abstract fun mirrorLinkDao(): MirrorLinkDao
    abstract fun inspectionListingDao(): InspectionListingDao
    abstract fun tenancyListingDao(): TenancyListingDao
    abstract fun propertyRemovalDao(): PropertyRemovalDao
    abstract fun propertyListingDao(): PropertyListingDao
    abstract fun manifestListingDao(): ManifestListingDao
    abstract fun reportListingDao(): ReportListingDao
}

@Suppress("KotlinNoActualForExpect")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}
