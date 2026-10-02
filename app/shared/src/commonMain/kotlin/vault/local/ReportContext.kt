package vault.local

import kotlinx.serialization.Serializable

@Serializable
data class ReportContext(
    val contractReference: String = "", val inspectionDate: String = "",
    val organization: String = "", val summary: String = "", val keywords: String = "",
    val documentsReviewed: String = "", val references: String = "",
) {
    fun validate(professional: Boolean) {
        require(inspectionDate.isBlank() || validReportDate(inspectionDate)) { "invalid inspection date: use YYYY-MM-DD" }
        if (professional) require(listOf(inspectionDate, summary, references).all(String::isNotBlank)) { "complete professional date, summary and references" }
    }
}

@Serializable
data class ProfessionalReview(
    val attributionsAcknowledged: Boolean = false,
    val registeredResponsibilityAcknowledged: Boolean = false,
    val standardsApplicabilityAcknowledged: Boolean = false,
    val consultationSource: String = "", val consultationDate: String = "",
) {
    fun validate() {
        require(attributionsAcknowledged && registeredResponsibilityAcknowledged && standardsApplicabilityAcknowledged) { "professional applicability acknowledgement required" }
        require(consultationSource.isBlank() == consultationDate.isBlank()) { "provide consultation source and date together" }
        require(consultationDate.isBlank() || validReportDate(consultationDate)) { "invalid consultation date" }
        require(consultationSource.length <= 1000) { "consultation source too long" }
    }
}

internal fun validReportDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val (year, month, day) = value.split('-').map(String::toInt)
    if (year !in 1900..9999 || month !in 1..12) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    return day in 1..days[month - 1]
}
