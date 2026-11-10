package vault.local

// A4 positions in points, measured from the top left of the page.
const val PDF_PAGE_WIDTH_PT = 595.0
const val PDF_PAGE_HEIGHT_PT = 842.0
const val PDF_MARGIN_LEFT_PT = 85.039
const val PDF_MARGIN_RIGHT_PT = 57.039
const val PDF_MARGIN_TOP_PT = 85.039
const val PDF_MARGIN_BOTTOM_PT = 56.693
const val PDF_MAX_PHOTO_HEIGHT_PT = 340.0
const val PDF_TITLE_SIZE = 28.0
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
    // Frozen manual display correction (degrees clockwise). Originals stay
    // immutable; renderers draw the rotated derivative in the sized slot.
    val displayRotation: Int = 0,
    // Short on-photo stamp (capture time, reported place, device GPS),
    // drawn over the photo rect by renderers. Snapshot presentation only.
    val stampLines: List<String> = emptyList(),
    val category: String? = null,
)

data class PdfFindingEntry(
    val evidenceRef: String,
    val roomLabel: String,
    val preset: String,
    val note: String?,
    val evidenceId: String? = null,
    val number: Int = 0,
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
    val inspectionKind: String = "entry",
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
    val byId = evidences.associateBy { it.id }
    val byRoom = evidences.groupBy { it.roomId }
    val findingsByRoom = input.findings.groupBy { it.roomId }
    var findingNumber = 0
    val sections = rooms.map { room ->
        val roomEvidence = byRoom[room.id].orEmpty()
        val photos = roomEvidence.map { e ->
            PdfPhotoEntry(
                evidenceId = e.id,
                roomLabel = room.label,
                sequence = e.sequence,
                caption = "${room.label} — nº ${e.sequence}",
                claimLine = buildString {
                    append(e.reportClaimLines())
                    input.photoDisplays[e.id]?.let {
                        append("\nImagem codificada ${it.encodedWidth}×${it.encodedHeight} px · exibida ")
                        append("${it.displayWidth}×${it.displayHeight} px · orientação EXIF ${it.orientation}")
                    }
                },
                displayRotation = e.displayRotation,
                stampLines = photoStampLines(e, address),
                category = e.shotCategory,
            )
        }
        val findings = findingsByRoom[room.id].orEmpty()
            .sortedWith(compareBy({ byId.getValue(it.evidenceId).sequence }, { it.id }))
            .map { f ->
                PdfFindingEntry(evidenceReference(byId.getValue(f.evidenceId).sequence), room.label,
                    input.findingLabels[f.preset] ?: f.preset, f.note, f.evidenceId, ++findingNumber)
            }
        val captured = roomEvidence.mapNotNull { it.shotCategory }.toSet()
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
        inspectionKind = input.inspection.kind,
    )
}

private fun methodologyLines(): List<String> = listOf(
    "Originais preservados. Cobertura por categorias distintas. Correções usam ocorrência/aditamento.",
    "Hora e GPS são declarações do dispositivo. Coordenadas EXIF são apresentadas separadamente.",
)

internal fun Int.pdfNumber(): String = toString().padStart(2, '0')
internal fun evidenceReference(sequence: Int): String = "E${sequence.pdfNumber()}"

internal val PdfModel.inspectionLabel: String get() = when {
    details.profile == "technical" -> "Vistoria técnica"
    inspectionKind == "exit" -> "Vistoria de saída"
    else -> "Vistoria de entrada"
}

internal val PdfModel.municipality: String get() =
    listOf(details.propertyMunicipality, details.propertyState).filter { it.isNotBlank() }.joinToString(" / ")
