package vault.local

import vault.local.db.LocalEvidence

// Shared PDF layout contract (L6). The model and photo pagination live
// here, identically for Android and iOS; platform code only draws the
// emitted items with the shared style tokens. Text flows platform-side;
// photo slots carry exact aspect-preserving rects so every sealed
// Evidence is represented without clipping or omission.
const val PDF_PAGE_WIDTH_PT = 595.0
const val PDF_PAGE_HEIGHT_PT = 842.0
const val PDF_MARGIN_LEFT_PT = 85.039
const val PDF_MARGIN_RIGHT_PT = 56.693
const val PDF_MARGIN_TOP_PT = 85.039
const val PDF_MARGIN_BOTTOM_PT = 56.693
const val PDF_MAX_PHOTO_HEIGHT_PT = 300.0
const val PDF_TITLE_SIZE = 20.0
const val PDF_HEADING_SIZE = 16.0
const val PDF_BODY_SIZE = 12.0
const val PDF_SMALL_SIZE = 9.0

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
    val missingCategories: List<String> = emptyList(),
    val notApplicableCategories: List<String> = emptyList(),
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
    val details: ReportDetails = ReportDetails(),
    val photoDisplays: Map<String, PhotoDisplay> = emptyMap(),
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
                claimLine = e.reportClaimLines(),
            )
        }
        val findings = input.findings.filter { it.roomId == room.id }.map { f ->
            PdfFindingEntry("nº ${evidences.firstOrNull { it.id == f.evidenceId }?.sequence ?: 0}", room.label, input.findingLabels[f.preset] ?: f.preset, f.note)
        }
        val captured = evidences.filter { it.roomId == room.id }.mapNotNull { it.shotCategory }.toSet()
        PdfRoomSection(room.label, coverageOf(room.id), photos, findings,
            applicableCategories(room.naCategories).filter { it.name !in captured }.map { reportShotLabel(it.name) },
            parseNaCategories(room.naCategories).map { reportShotLabel(it.name) })
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
        details = input.details,
        photoDisplays = input.photoDisplays,
    )
}

private fun methodologyLines(): List<String> = listOf(
    "Originais preservados sem recompressão; a origem declarada aparece em cada foto.",
    "Cobertura calculada por categorias distintas de foto, nunca por contagem.",
    "Hora e GPS são declarações do dispositivo, sem certificação independente.",
    "Coordenadas EXIF são declaração separada e divergências ficam visíveis.",
    "Lacre terminal: alterações posteriores usam ocorrência/aditamento.",
)
