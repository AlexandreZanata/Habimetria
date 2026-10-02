package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vault.local.db.InspectionReportDetails
import vault.local.db.VaultDatabase

// User declarations reviewed before sealing; never verified civil identity.
@Serializable
data class ReportDetails(
    val profile: String = "rental",
    val authorName: String = "",
    val participants: String = "",
    val purpose: String = "",
    val scope: String = "",
    val methods: String = "",
    val limitations: String = "",
    val analysis: String = "",
    val conclusions: String = "",
    val recommendations: String = "",
    val council: String = "",
    val councilState: String = "",
    val registration: String = "",
    val responsibilityType: String = "",
    val responsibilityReference: String = "",
    val standards: String = "",
    val authorAcknowledged: Boolean = false,
) {
    fun validate(kind: String, forSeal: Boolean = true): ReportDetails {
        require(profile in listOf("rental", "technical")) { "unsupported report profile" }
        require((kind == "technical") == (profile == "technical")) { "report profile does not match inspection" }
        require(reportDetailsJson.encodeToString(this).length <= 100_000) { "report details too long" }
        if (profile == "technical" && forSeal) {
            require(listOf(authorName, purpose, scope, methods, limitations, analysis, conclusions,
                registration, responsibilityReference).all { it.isNotBlank() }) { "complete professional report details" }
            require(councilState.matches(Regex("[A-Z]{2}")) && councilState in BRAZIL_STATES) { "invalid council state" }
            require((council == "CREA" && responsibilityType == "ART") ||
                (council == "CAU" && responsibilityType == "RRT")) { "council and ART/RRT mismatch" }
            require(authorAcknowledged) { "author acknowledgement required" }
        }
        return this
    }
}

private val BRAZIL_STATES = setOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
    "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")
internal val reportDetailsJson = Json { encodeDefaults = true }

class ReportDetailsRepository(private val db: VaultDatabase) {
    suspend fun frozen(inspectionId: String): ReportDetails = db.manifestDao().getByInspection(inspectionId)?.let { manifestSnapshot(it).details } ?: get(inspectionId)

    suspend fun get(inspectionId: String): ReportDetails {
        val row = db.inspectionReportDetailsDao().get(inspectionId)
        return row?.let { reportDetailsJson.decodeFromString<ReportDetails>(it.json) }
            ?: ReportDetails(profile = if (db.inspectionDao().get(inspectionId)?.kind == "technical") "technical" else "rental")
    }

    suspend fun save(inspectionId: String, details: ReportDetails) = localWrite {
        val inspection = LocalWriteGuard(db).requireDraft(inspectionId)
        details.validate(inspection.kind, forSeal = false)
        db.inspectionReportDetailsDao().save(InspectionReportDetails(inspectionId, reportDetailsJson.encodeToString(details)))
    }
}
