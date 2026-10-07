package vault.local

import kotlinx.serialization.json.*
import vault.local.db.*

// Reads only the signed canonical payload; no live Property/Room/Findings.
fun manifestSnapshot(manifest: LocalManifest): ManifestInput {
    require(manifest.canonicalJson.encodeToByteArray().size <= 8 * 1024 * 1024) { "manifest too large" }
    require(manifestSha256(manifest.canonicalJson) == manifest.canonicalSha256) { "manifest digest mismatch" }
    val root = Json.parseToJsonElement(manifest.canonicalJson).jsonObject
    val version = root.getValue("formatVersion").jsonPrimitive.int
    require(version in 1..MANIFEST_FORMAT_VERSION && version == manifest.formatVersion) { "unsupported manifest" }
    require(root.text("purpose") == MANIFEST_PURPOSE) { "wrong manifest purpose" }
    val inspectionId = root.text("inspectionId")
    require(inspectionId == manifest.inspectionId) { "inspection binding mismatch" }
    val propertyId = root.text("propertyId")
    val tenancyId = root.optionalText("tenancyId")
    val sealedAt = root.text("sealedAtDevice")
    require(sealedAt == manifest.sealedAtDevice) { "seal timestamp binding mismatch" }
    val rooms = root.getValue("rooms").jsonArray.map { item ->
        val row = item.jsonObject
        LocalRoom(row.text("id"), inspectionId, row.text("roomKey"), row.text("label"),
            row.getValue("sort").jsonPrimitive.int, row.text("naCategories"))
    }
    val evidences = root.getValue("evidences").jsonArray.map { item ->
        val row = item.jsonObject
        LocalEvidence(
            id = row.text("id"), inspectionId = inspectionId, roomId = row.optionalText("roomId"),
            fileName = row.text("fileName"), mime = row.text("mime"), size = row.getValue("size").jsonPrimitive.long,
            sha256 = row.text("sha256"), sequence = row.getValue("sequence").jsonPrimitive.int,
            capturedAtDevice = row.text("capturedAtDevice"), origin = row.text("origin"),
            shotCategory = row.optionalText("shotCategory"), latitude = row.number("latitude"),
            longitude = row.number("longitude"), accuracyM = row.number("accuracyM")?.toFloat(),
            locationStatus = row.text("locationStatus"), fixAgeMs = row.optionalText("fixAgeMs")?.toLong(),
            provider = row.optionalText("provider"), accuracyLevel = row.optionalText("accuracyLevel"),
            isMocked = row.getValue("isMocked").jsonPrimitive.boolean,
            exifLatitude = row.number("exifLatitude"), exifLongitude = row.number("exifLongitude"),
            mockSignal = row.optionalText("mockSignal"), fixTimeDevice = row.optionalText("fixTimeDevice"),
            deviceUtcOffsetMinutes = row.optionalText("deviceUtcOffsetMinutes")?.toInt(),
            exifOrientation = row.optionalText("exifOrientation")?.toInt(),
        )
    }
    val findings = root.getValue("findings").jsonArray.map { item ->
        val row = item.jsonObject
        LocalFinding(row.text("id"), row.text("roomId"), row.text("evidenceId"),
            row.text("preset"), row.optionalText("note"))
    }
    require(rooms.map { it.id }.distinct().size == rooms.size) { "duplicate rooms" }
    require(evidences.isNotEmpty() && evidences.map { it.id }.distinct().size == evidences.size) { "invalid evidences" }
    require(evidences.map { it.sequence }.distinct().size == evidences.size) { "duplicate sequences" }
    val roomIds = rooms.map { it.id }.toSet()
    evidences.forEach {
        require(safeArtifactId(it.id) && it.roomId in roomIds && it.size in 1..MAX_ORIGINAL_BYTES) { "invalid evidence" }
        require(it.sha256.matches(Regex("[0-9a-f]{64}"))) { "invalid evidence digest" }
    }
    val evidenceRooms = evidences.associate { it.id to it.roomId }
    findings.forEach { require(it.roomId == evidenceRooms[it.evidenceId]) { "finding binding mismatch" } }
    require(findings.map { it.id }.distinct().size == findings.size) { "duplicate findings" }
    return ManifestInput(propertyId, root.text("address"),
        tenancyId?.let { LocalTenancy(it, propertyId, root.text("tenancyType"), sealedAt) },
        LocalInspection(inspectionId, tenancyId, root.optionalText("kind") ?: "entry", STATUS_SEALED, sealedAt, sealedAt, propertyId),
        rooms, evidences, findings, sealedAt,
        root["reportDetails"]?.let { reportDetailsJson.decodeFromJsonElement(ReportDetails.serializer(), it) } ?: ReportDetails(),
        root["findingLabels"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty())
}

private fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content
private fun JsonObject.optionalText(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull
private fun JsonObject.number(name: String): Double? = optionalText(name)?.toDouble()

internal const val MAX_ORIGINAL_BYTES = 50L * 1024 * 1024
internal fun safeArtifactId(id: String): Boolean = id.matches(Regex("[A-Za-z0-9_-]{1,128}"))

fun snapshotCoverage(input: ManifestInput, room: LocalRoom): Int {
    val applicable = applicableCategories(room.naCategories).map { it.name }.toSet()
    if (applicable.isEmpty()) return 100
    val captured = input.evidences.filter { it.roomId == room.id }
        .mapNotNull { it.shotCategory }.toSet().intersect(applicable)
    return captured.size * 100 / applicable.size
}
