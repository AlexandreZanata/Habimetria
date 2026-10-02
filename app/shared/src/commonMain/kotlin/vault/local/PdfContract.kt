package vault.local

import vault.local.db.LocalEvidence

// Shared PDF layout contract (L6). The model and photo pagination live
// here, identically for Android and iOS; platform code only draws the
// emitted items with the shared style tokens. Text flows platform-side;
// photo slots carry exact aspect-preserving rects so every sealed
// Evidence is represented without clipping or omission.
const val PDF_PAGE_WIDTH_PT = 595.0
const val PDF_PAGE_HEIGHT_PT = 842.0
const val PDF_MARGIN_LEFT_PT = 40.0
const val PDF_MARGIN_RIGHT_PT = 40.0
const val PDF_MARGIN_TOP_PT = 64.0
const val PDF_MARGIN_BOTTOM_PT = 56.0
const val PDF_MAX_PHOTO_HEIGHT_PT = 300.0
const val PDF_TITLE_SIZE = 20.0
const val PDF_HEADING_SIZE = 16.0
const val PDF_BODY_SIZE = 10.0
const val PDF_SMALL_SIZE = 8.0

// One entry per sealed Evidence, in manifest sequence order.
data class PdfPhotoEntry(
    val evidenceId: String,
    val roomLabel: String,
    val sequence: Int,
    val caption: String,
    val claimLine: String?,
)

data class PdfFindingEntry(
    val evidenceRef: String,
    val roomLabel: String,
    val preset: String,
    val note: String?,
)

data class PdfRoomSection(
    val label: String,
    val coveragePercent: Int,
    val photos: List<PdfPhotoEntry>,
    val findings: List<PdfFindingEntry>,
)

data class PdfModel(
    val reportId: String,
    val address: String,
    val inspectionId: String,
    val manifestId: String,
    val manifestSha256: String,
    val sealedAtDevice: String,
    val signerKeyId: String,
    val rooms: List<PdfRoomSection>,
    val methodology: List<String>,
    val appendixHashes: List<Pair<String, String>>,
)

// Fixed model assembly from the sealed snapshot: rooms in sort order,
// photos in manifest sequence, findings grouped by room.
fun buildPdfModel(
    reportId: String,
    address: String,
    input: ManifestInput,
    manifestId: String,
    manifestSha256: String,
    signerKeyId: String,
    coverageOf: (String) -> Int,
): PdfModel {
    val rooms = input.rooms.sortedWith(compareBy({ it.sort }, { it.id }))
    val evidences = input.evidences.sortedWith(compareBy({ it.sequence }, { it.id }))
    val sections = rooms.map { room ->
        val photos = evidences.filter { it.roomId == room.id }.map { e ->
            PdfPhotoEntry(
                evidenceId = e.id,
                roomLabel = room.label,
                sequence = e.sequence,
                caption = "${room.label} — nº ${e.sequence}",
                claimLine = e.claimLine(),
            )
        }
        val findings = input.findings.filter { it.roomId == room.id }.map { f ->
            PdfFindingEntry("nº ${evidences.firstOrNull { it.id == f.evidenceId }?.sequence ?: 0}", room.label, f.preset, f.note)
        }
        PdfRoomSection(room.label, coverageOf(room.id), photos, findings)
    }
    val appendix = buildList {
        add("manifest" to manifestSha256)
        evidences.forEach { add("evidence ${it.id}" to it.sha256) }
    }
    return PdfModel(
        reportId = reportId,
        address = address,
        inspectionId = input.inspection.id,
        manifestId = manifestId,
        manifestSha256 = manifestSha256,
        sealedAtDevice = input.sealedAtDevice,
        signerKeyId = signerKeyId,
        rooms = sections,
        methodology = methodologyLines(),
        appendixHashes = appendix,
    )
}

private fun LocalEvidence.claimLine(): String? {
    if (latitude == null || longitude == null) return null
    val acc = accuracyM?.let { " ±${it.toInt()} m" } ?: ""
    return "${canonicalDouble(latitude)}, ${canonicalDouble(longitude)}$acc (${locationStatus.lowercase()})"
}

private fun methodologyLines(): List<String> = listOf(
    "Fotos capturadas no dispositivo e preservadas sem recompressão.",
    "Cobertura calculada por categorias distintas de foto, nunca por contagem.",
    "Hora e GPS são declarações do dispositivo, sem certificação independente.",
    "Coordenadas EXIF são declaração separada e divergências ficam visíveis.",
    "Lacre terminal: alterações posteriores usam ocorrência/aditamento.",
)

