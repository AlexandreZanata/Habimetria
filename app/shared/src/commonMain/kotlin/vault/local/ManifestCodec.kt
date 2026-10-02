package vault.local

import okio.ByteString.Companion.encodeUtf8
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.math.round

// Canonical manifest: byte-exact serialization with fixed field order,
// explicit sequence ordering and deterministic number formatting.
// The SHA-256 over these bytes is the snapshot identity; signatures
// cover exactly these bytes with format version and purpose bound in.
// v2 binds the inspection kind so entry and exit snapshots differ.
const val MANIFEST_FORMAT_VERSION = 3
const val MANIFEST_PURPOSE = "local-seal/v1"

data class ManifestInput(
    val propertyId: String,
    val address: String,
    val tenancy: LocalTenancy?,
    val inspection: LocalInspection,
    val rooms: List<LocalRoom>,
    val evidences: List<LocalEvidence>,
    val findings: List<LocalFinding>,
    val sealedAtDevice: String,
    val details: ReportDetails = ReportDetails(),
    val findingLabels: Map<String, String> = emptyMap(),
)

fun canonicalManifestJson(input: ManifestInput): String {
    val rooms = input.rooms.sortedWith(compareBy({ it.sort }, { it.id }))
    val evidences = input.evidences.sortedWith(compareBy({ it.sequence }, { it.id }))
    val findings = input.findings.sortedWith(compareBy({ it.evidenceId }, { it.id }))
    val sb = StringBuilder()
    sb.append("{\"address\":")
    sb.appendQuoted(input.address)
    sb.append(",\"evidences\":[")
    evidences.forEachIndexed { i, e ->
        if (i > 0) sb.append(",")
        sb.append("{\"accuracyLevel\":")
        sb.appendNullableQuoted(e.accuracyLevel)
        sb.append(",\"accuracyM\":")
        sb.appendNullableNumber(e.accuracyM?.let { canonicalFloat(it) })
        sb.append(",\"capturedAtDevice\":")
        sb.appendQuoted(e.capturedAtDevice)
        sb.append(",\"exifLatitude\":")
        sb.appendNullableNumber(e.exifLatitude?.let { canonicalDouble(it) })
        sb.append(",\"exifLongitude\":")
        sb.appendNullableNumber(e.exifLongitude?.let { canonicalDouble(it) })
        sb.append(",\"fileName\":")
        sb.appendQuoted(e.fileName)
        sb.append(",\"fixAgeMs\":")
        sb.appendNullableNumber(e.fixAgeMs?.toString())
        sb.append(",\"id\":")
        sb.appendQuoted(e.id)
        sb.append(",\"isMocked\":")
        sb.append(if (e.isMocked) "true" else "false")
        sb.append(",\"mockSignal\":")
        sb.appendNullableQuoted(e.mockSignal)
        sb.append(",\"fixTimeDevice\":")
        sb.appendNullableQuoted(e.fixTimeDevice)
        sb.append(",\"deviceUtcOffsetMinutes\":")
        sb.appendNullableNumber(e.deviceUtcOffsetMinutes?.toString())
        sb.append(",\"exifOrientation\":")
        sb.appendNullableNumber(e.exifOrientation?.toString())
        sb.append(",\"latitude\":")
        sb.appendNullableNumber(e.latitude?.let { canonicalDouble(it) })
        sb.append(",\"locationStatus\":")
        sb.appendQuoted(e.locationStatus)
        sb.append(",\"longitude\":")
        sb.appendNullableNumber(e.longitude?.let { canonicalDouble(it) })
        sb.append(",\"mime\":")
        sb.appendQuoted(e.mime)
        sb.append(",\"origin\":")
        sb.appendQuoted(e.origin)
        sb.append(",\"provider\":")
        sb.appendNullableQuoted(e.provider)
        sb.append(",\"roomId\":")
        sb.appendNullableQuoted(e.roomId)
        sb.append(",\"sequence\":")
        sb.append(e.sequence)
        sb.append(",\"sha256\":")
        sb.appendQuoted(e.sha256)
        sb.append(",\"shotCategory\":")
        sb.appendNullableQuoted(e.shotCategory)
        sb.append(",\"size\":")
        sb.append(e.size)
        sb.append("}")
    }
    sb.append("],\"reportDetails\":")
    sb.append(reportDetailsJson.encodeToString(ReportDetails.serializer(), input.details))
    sb.append(",\"findingLabels\":{")
    input.findingLabels.entries.sortedBy { it.key }.forEachIndexed { i, entry ->
        if (i > 0) sb.append(",")
        sb.appendQuoted(entry.key)
        sb.append(":")
        sb.appendQuoted(entry.value)
    }
    sb.append("},\"findings\":[")
    findings.forEachIndexed { i, f ->
        if (i > 0) sb.append(",")
        sb.append("{\"evidenceId\":")
        sb.appendQuoted(f.evidenceId)
        sb.append(",\"id\":")
        sb.appendQuoted(f.id)
        sb.append(",\"note\":")
        sb.appendNullableQuoted(f.note)
        sb.append(",\"preset\":")
        sb.appendQuoted(f.preset)
        sb.append(",\"roomId\":")
        sb.appendQuoted(f.roomId)
        sb.append("}")
    }
    sb.append("],\"formatVersion\":")
    sb.append(MANIFEST_FORMAT_VERSION)
    sb.append(",\"inspectionId\":")
    sb.appendQuoted(input.inspection.id)
    sb.append(",\"kind\":")
    sb.appendQuoted(input.inspection.kind)
    sb.append(",\"propertyId\":")
    sb.appendQuoted(input.propertyId)
    sb.append(",\"purpose\":")
    sb.appendQuoted(MANIFEST_PURPOSE)
    sb.append(",\"rooms\":[")
    rooms.forEachIndexed { i, r ->
        if (i > 0) sb.append(",")
        sb.append("{\"id\":")
        sb.appendQuoted(r.id)
        sb.append(",\"label\":")
        sb.appendQuoted(r.label)
        sb.append(",\"naCategories\":")
        sb.appendQuoted(r.naCategories)
        sb.append(",\"roomKey\":")
        sb.appendQuoted(r.roomKey)
        sb.append(",\"sort\":")
        sb.append(r.sort)
        sb.append("}")
    }
    sb.append("],\"sealedAtDevice\":")
    sb.appendQuoted(input.sealedAtDevice)
    sb.append(",\"tenancyId\":")
    sb.appendNullableQuoted(input.tenancy?.id)
    sb.append(",\"tenancyType\":")
    sb.appendNullableQuoted(input.tenancy?.type)
    sb.append("}")
    return sb.toString()
}

fun manifestSha256(canonicalJson: String): String =
    canonicalJson.encodeUtf8().sha256().hex()
