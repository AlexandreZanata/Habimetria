package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.InspectionBlockReason
import vault.local.db.InspectionDeleteOutcome
import vault.local.db.InspectionRemovalPlan
import vault.local.db.VaultDatabase

// Inspection removal use cases: DRAFT and SEALED delete through the
// same transactional path, LOCAL_ONLY, no network. Rows commit first;
// files clean up after and never gate the result (startup reconcile
// sweeps orphans). Tenancies, properties and occurrences survive:
// deleting inspections never unblocks property deletion by itself.
class InspectionRemovalRepository(
    private val db: VaultDatabase,
    private val originals: OriginalStore,
    private val derivatives: DerivativeStore,
    private val filesDir: String,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    // Fresh wording snapshot for the dialog (draft vs sealed copy).
    suspend fun eligibility(inspectionId: String): InspectionRemovalPlan =
        db.inspectionRemovalDao().planFor(inspectionId)

    suspend fun delete(inspectionId: String): InspectionDeleteOutcome {
        val plan = db.inspectionRemovalDao().planFor(inspectionId)
        if (plan is InspectionRemovalPlan.Missing) return InspectionDeleteOutcome.Missing
        if (plan is InspectionRemovalPlan.Blocked) {
            return InspectionDeleteOutcome.Blocked(plan.reason)
        }
        val ready = plan as InspectionRemovalPlan.Ready
        val document = ReportDetailsRepository(db).get(inspectionId).responsibilityDocument
        return when (db.inspectionRemovalDao().commitRemoval(inspectionId)) {
            InspectionDeleteOutcome.Missing -> InspectionDeleteOutcome.Missing
            is InspectionDeleteOutcome.Blocked -> InspectionDeleteOutcome.Blocked(
                InspectionBlockReason.HAS_COMPARISONS,
            )
            InspectionDeleteOutcome.Deleted -> {
                ready.evidenceIds.forEach { evidenceId ->
                    originals.deleteOriginal(evidenceId)
                    derivatives.deleteDerivatives(evidenceId)
                }
                // Same layout as ReportGenerator.reportsDir(); the row is
                // already gone, so missing files are simply skipped.
                ready.reportIds.forEach { reportId ->
                    fs.delete("$filesDir/vault/reports/$reportId.pdf".toPath(), mustExist = false)
                    fs.delete("$filesDir/vault/reports/$reportId-receipt.json".toPath(), mustExist = false)
                    require(safeArtifactId(reportId)) { "invalid report id" }
                    fs.deleteRecursively("$filesDir/vault/packages/$reportId".toPath())
                    fs.delete("$filesDir/vault/packages/$reportId.zip".toPath(), mustExist = false)
                    fs.deleteRecursively("$filesDir/vault/declarations/$reportId".toPath())
                }
                document?.let { fs.delete(ResponsibilityDocuments(filesDir, fs).path(it.id).toPath(), mustExist = false) }
                InspectionDeleteOutcome.Deleted
            }
        }
    }
}
