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
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Inspection(
    val id: String,
    @SerialName("tenancy_id") val tenancyId: String,
    val kind: String,
    val status: String,
    @SerialName("created_at") val createdAt: String = "",
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
    @SerialName("sealed_at") val sealedAt: String = "",
    @SerialName("evidence_count") val evidenceCount: Int = 0,
    @SerialName("manifest_sha256") val manifestSha256: String = "",
    val signature: String = "",
)

@Serializable
data class Evidence(
    val id: String,
    @SerialName("inspection_id") val inspectionId: String,
    @SerialName("received_at_server") val receivedAtServer: String,
    val sequence: Int,
    @SerialName("room_id") val roomId: String? = null,
    val type: String = "",
    val mime: String = "",
    val size: Long = 0,
    val sha256: String = "",
)

@Serializable
data class RoomCoverageView(
    @SerialName("room_id") val roomId: String,
    val key: String,
    val label: String,
    val photos: Int,
    val percent: Int,
)

@Serializable
data class Coverage(
    @SerialName("inspection_id") val inspectionId: String,
    @SerialName("total_percent") val totalPercent: Int,
    val rooms: List<RoomCoverageView>,
)

@Serializable
data class Finding(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("evidence_id") val evidenceId: String,
    val preset: String,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class FindingBody(
    @SerialName("evidence_id") val evidenceId: String,
    val preset: String,
    val note: String? = null,
)

@Serializable
data class VerifyResult(
    val authentic: Boolean,
    @SerialName("verify_code") val verifyCode: String,
    @SerialName("sealed_at") val sealedAt: String,
    @SerialName("evidence_count") val evidenceCount: Int,
    @SerialName("manifest_sha256") val manifestSha256: String,
    @SerialName("signature_valid") val signatureValid: Boolean,
    @SerialName("report_sha256") val reportSha256: String? = null,
    @SerialName("pdf_ready") val pdfReady: Boolean = false,
)
