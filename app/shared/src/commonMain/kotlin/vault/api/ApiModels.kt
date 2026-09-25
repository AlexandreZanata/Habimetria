package vault.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: String,
    val address: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Tenancy(
    val id: String,
    @SerialName("property_id") val propertyId: String,
    val type: String,
)

@Serializable
data class Inspection(
    val id: String,
    @SerialName("tenancy_id") val tenancyId: String,
    val kind: String,
    val status: String,
)

@Serializable
data class Room(
    val id: String,
    @SerialName("inspection_id") val inspectionId: String,
    val key: String,
    val label: String,
    val sort: Int,
)

@Serializable
data class SealResult(
    @SerialName("inspection_id") val inspectionId: String,
    val status: String,
    @SerialName("manifest_sha256") val manifestSha256: String,
    val signature: String,
    @SerialName("report_id") val reportId: String,
    @SerialName("verify_code") val verifyCode: String,
    @SerialName("evidence_count") val evidenceCount: Int,
)

@Serializable
data class ReportDetail(
    val report: Report,
    @SerialName("verify_url") val verifyUrl: String,
)

@Serializable
data class Report(
    @SerialName("report_id") val id: String,
    @SerialName("inspection_id") val inspectionId: String,
    @SerialName("verify_code") val verifyCode: String,
    @SerialName("report_sha256") val reportSha256: String? = null,
    @SerialName("pdf_ready") val pdfReady: Boolean = false,
)
