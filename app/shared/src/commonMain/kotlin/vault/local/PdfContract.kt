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

// Photo pagination: exact aspect-preserving rects in points.
data class PdfRect(val x: Double, val y: Double, val w: Double, val h: Double)

sealed interface PageItem {
    data class Photo(val entry: PdfPhotoEntry, val rect: PdfRect) : PageItem
    data class Text(val kind: TextKind, val text: String) : PageItem
    data class Finding(val entry: PdfFindingEntry) : PageItem
    data class HashRow(val label: String, val hex: String) : PageItem
    // QR identifies Report/Manifest/format for local import. It never
    // certifies bytes on its own; the detached receipt does that.
    data class Qr(val content: String) : PageItem
}

class RenderCancelledException : IllegalStateException("render cancelled")

// Platform drawing port. Photos arrive through loadPhoto (preview bytes
// preferred, bounded decode inside). Cancellation is cooperative.
interface PlanRenderer {
    suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (done: Int, total: Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray
}

// QR payload: identity triple, never a LAN URL, never proof of integrity.
fun qrContent(reportId: String, manifestSha256: String): String =
    "vault-report/v1/$reportId/$manifestSha256"

enum class TextKind { TITLE, HEADING, BODY, SMALL }

data class RenderPage(val number: Int, val header: String, val items: List<PageItem>)

data class ImageDims(val width: Int, val height: Int)

// Dimensions without full decode (bounded memory); null when undecodable.
expect fun imageDimensions(bytes: ByteArray): ImageDims?

// Display-only seal timestamp: "01/10/2026 às 12:10" in the device
// timezone. The raw ISO stays in the model and manifest; unparsable
// input falls back to the raw string so the PDF never breaks.
expect fun formatSealedAtDevice(iso: String): String

fun contentWidthPt(): Double = PDF_PAGE_WIDTH_PT - PDF_MARGIN_LEFT_PT - PDF_MARGIN_RIGHT_PT

fun bottomLimitPt(): Double = PDF_PAGE_HEIGHT_PT - PDF_MARGIN_BOTTOM_PT

// Paginates the whole model. Every photo and finding lands exactly once;
// pages carry sequential numbers starting at 1 (cover).
fun paginateModel(
    model: PdfModel,
    dimsOf: (evidenceId: String) -> ImageDims?,
): List<RenderPage> {
    val pages = mutableListOf<RenderPage>()
    var number = 0
    fun newPage(): MutableList<PageItem> {
        number++
        return mutableListOf()
    }
    var current = newPage()
    fun flush() {
        pages.add(RenderPage(number, model.reportId, current.toList()))
        current = newPage()
    }
    // Cover.
    current.add(PageItem.Text(TextKind.TITLE, "Relatório de Vistoria"))
    current.add(PageItem.Text(TextKind.BODY, model.address))
    current.add(PageItem.Text(TextKind.BODY, "Inspeção ${model.inspectionId}"))
    current.add(PageItem.Text(TextKind.BODY, "Lacrado (dispositivo): ${formatSealedAtDevice(model.sealedAtDevice)}"))
    current.add(PageItem.Text(TextKind.BODY, "Manifesto ${model.manifestSha256.take(16)}…"))
    current.add(PageItem.Text(TextKind.BODY, "Chave ${model.signerKeyId.take(16)}…"))
    current.add(PageItem.Qr(qrContent(model.reportId, model.manifestSha256)))
    current.add(
        PageItem.Text(
            TextKind.SMALL,
            "O QR identifica o relatório. A integridade se verifica no app com o recibo assinado.",
        ),
    )
    flush()
    // Rooms.
    for (room in model.rooms) {
        current.add(PageItem.Text(TextKind.HEADING, "${room.label} — ${room.coveragePercent}%"))
        var cursorY = PDF_MARGIN_TOP_PT + 24.0
        for (photo in room.photos) {
            val dims = dimsOf(photo.evidenceId) ?: ImageDims(4, 3)
            val aspect = dims.width.toDouble() / dims.height.toDouble().coerceAtLeast(1.0)
            val width = contentWidthPt()
            val height = (width / aspect.coerceAtLeast(0.2)).coerceAtMost(PDF_MAX_PHOTO_HEIGHT_PT)
            if (cursorY + height > bottomLimitPt()) {
                flush()
                cursorY = PDF_MARGIN_TOP_PT
            }
            current.add(
                PageItem.Photo(
                    photo,
                    PdfRect(PDF_MARGIN_LEFT_PT, cursorY, width, height),
                ),
            )
            // Image plus single-line caption and claim (drawn by platform).
            cursorY += height + 30.0
        }
        for (finding in room.findings) current.add(PageItem.Finding(finding))
        if (current.isNotEmpty()) flush()
    }
    // Methodology.
    current.add(PageItem.Text(TextKind.HEADING, "Metodologia"))
    model.methodology.forEach { current.add(PageItem.Text(TextKind.BODY, it)) }
    flush()
    // Appendix: every digest in full.
    current.add(PageItem.Text(TextKind.HEADING, "Apêndice — resumos"))
    current.add(PageItem.Text(TextKind.BODY, "Manifesto ${model.manifestId}"))
    for ((label, hex) in model.appendixHashes) {
        if (current.size >= 40) flush()
        current.add(PageItem.HashRow(label, hex))
    }
    flush()
    return pages.dropLastWhile { it.items.isEmpty() }
}
