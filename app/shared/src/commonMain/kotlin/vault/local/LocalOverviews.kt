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
    // True when the owning Property is archived (U09): history stays
    // listed, the UI labels it instead of hiding it.
    val propertyArchived: Boolean = false,
)

data class ReportOverview(
    val report: LocalReport,
    val inspectionId: String,
    val propertyId: String?,
    val propertyLabel: String,
    val propertyArchived: Boolean = false,
)

// SEALED inspection without a READY report: the entry point for
// "Gerar PDF". Never produced for DRAFT (seal eligibility) and never
// presented as an existing report.
data class PendingPdf(
    val inspectionId: String,
    val propertyId: String?,
    val propertyLabel: String,
    val propertyArchived: Boolean = false,
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
        val labels = propertyLabelsFor(inspections)
        return inspections.map { inspection ->
            val link = labels[inspection.id]
            InspectionOverview(inspection, link?.first, link?.second.orEmpty(), link?.third == true)
        }
    }

    suspend fun reportOverviews(limit: Int = 100, offset: Int = 0): List<ReportOverview> {
        val reports = db.reportListingDao()
            .listPaged(limit.coerceIn(1, 200), offset.coerceAtLeast(0))
        if (reports.isEmpty()) return emptyList()
        val inspections = db.inspectionListingDao().listByIds(reports.map { it.inspectionId })
            .associateBy { it.id }
        val labels = propertyLabelsFor(inspections.values.toList())
        return reports.map { report ->
            val inspection = inspections[report.inspectionId]
            val link = inspection?.let { labels[it.id] }
            ReportOverview(report, report.inspectionId, link?.first, link?.second.orEmpty(), link?.third == true)
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
        val labels = propertyLabelsFor(pending)
        return pending.take(limit.coerceIn(1, 200)).map { inspection ->
            val link = labels[inspection.id]
            PendingPdf(inspection.id, link?.first, link?.second.orEmpty(), link?.third == true)
        }
    }

    // Inspection id -> property label, in at most two bulk queries.
    private suspend fun propertyLabelsFor(inspections: List<LocalInspection>): Map<String, Triple<String?, String, Boolean>> {
        val ids = inspections.mapNotNull { it.tenancyId }.distinct()
        val tenancies = if (ids.isEmpty()) emptyMap() else db.tenancyListingDao().listByIds(ids).associateBy { it.id }
        val owner = inspections.associate { it.id to (it.propertyId ?: tenancies[it.tenancyId]?.propertyId) }
        val properties = db.propertyListingDao().listByIds(owner.values.filterNotNull().distinct()).associateBy { it.id }
        return owner.mapValues { (_, id) ->
            val property = properties[id]
            Triple(property?.id, property?.nickname?.takeIf { it.isNotBlank() } ?: property?.address.orEmpty(), property?.archived == true)
        }
    }
}
