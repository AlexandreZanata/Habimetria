package vault.local

import vault.local.db.LocalInspection
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase

// Aggregated rows for the Vistorias/Relatórios destinations. Every
// listing resolves its property labels in bulk (≤5 queries); screens
// never loop per-row repository calls over 200-inspection fixtures.
data class InspectionOverview(
    val inspection: LocalInspection,
    val propertyId: String?,
    // Nickname or address; "" when the chain is gone (UI shows fallback).
    val propertyLabel: String,
)

data class ReportOverview(
    val report: LocalReport,
    val inspectionId: String,
    val propertyId: String?,
    val propertyLabel: String,
)

// SEALED inspection without a READY report: the entry point for
// "Gerar PDF". Never produced for DRAFT (seal eligibility) and never
// presented as an existing report.
data class PendingPdf(
    val inspectionId: String,
    val propertyId: String?,
    val propertyLabel: String,
)

class LocalOverviewRepository(private val db: VaultDatabase) {

    suspend fun inspectionOverviews(
        status: String? = null,
        limit: Int = 100,
        offset: Int = 0,
    ): List<InspectionOverview> {
        val boundedLimit = limit.coerceIn(1, 200)
        val safeOffset = offset.coerceAtLeast(0)
        val inspections = if (status == null) {
            db.inspectionListingDao().listPaged(boundedLimit, safeOffset)
        } else {
            db.inspectionListingDao().listPagedByStatus(status, boundedLimit, safeOffset)
        }
        if (inspections.isEmpty()) return emptyList()
        val labels = propertyLabelsFor(inspections.map { it.tenancyId })
        return inspections.map { inspection ->
            val link = labels[inspection.tenancyId]
            InspectionOverview(inspection, link?.first, link?.second.orEmpty())
        }
    }

    suspend fun reportOverviews(limit: Int = 100, offset: Int = 0): List<ReportOverview> {
        val reports = db.reportListingDao()
            .listPaged(limit.coerceIn(1, 200), offset.coerceAtLeast(0))
        if (reports.isEmpty()) return emptyList()
        val inspections = db.inspectionListingDao().listByIds(reports.map { it.inspectionId })
            .associateBy { it.id }
        val labels = propertyLabelsFor(inspections.values.map { it.tenancyId })
        return reports.map { report ->
            val inspection = inspections[report.inspectionId]
            val link = inspection?.let { labels[it.tenancyId] }
            ReportOverview(report, report.inspectionId, link?.first, link?.second.orEmpty())
        }
    }

    suspend fun pendingPdfs(limit: Int = 100): List<PendingPdf> {
        val sealed = db.inspectionListingDao().listPagedByStatus(STATUS_SEALED, 200, 0)
        if (sealed.isEmpty()) return emptyList()
        val manifests = db.manifestListingDao().listForInspections(sealed.map { it.id })
        val readyManifestIds = if (manifests.isEmpty()) {
            emptySet()
        } else {
            db.reportListingDao().listForManifests(manifests.map { it.id })
                .filter { it.status == REPORT_READY }
                .map { it.manifestId }
                .toSet()
        }
        val manifestByInspection = manifests.associateBy({ it.inspectionId }, { it.id })
        val pending = sealed.filter { readyManifestIds.none { id -> id == manifestByInspection[it.id] } }
        if (pending.isEmpty()) return emptyList()
        val labels = propertyLabelsFor(pending.map { it.tenancyId })
        return pending.take(limit.coerceIn(1, 200)).map { inspection ->
            val link = labels[inspection.tenancyId]
            PendingPdf(inspection.id, link?.first, link?.second.orEmpty())
        }
    }

    // Tenancy id -> (property id, display label) in 2 bulk queries.
    private suspend fun propertyLabelsFor(tenancyIds: List<String>): Map<String, Pair<String?, String>> {
        if (tenancyIds.isEmpty()) return emptyMap()
        val tenancies = db.tenancyListingDao().listByIds(tenancyIds.distinct())
        if (tenancies.isEmpty()) return emptyMap()
        val properties = db.propertyListingDao().listByIds(tenancies.map { it.propertyId }.distinct())
            .associateBy { it.id }
        return tenancies.associate { tenancy ->
            val property = properties[tenancy.propertyId]
            val label = property?.nickname?.takeIf { it.isNotBlank() } ?: property?.address.orEmpty()
            tenancy.id to (property?.id to label)
        }
    }
}
