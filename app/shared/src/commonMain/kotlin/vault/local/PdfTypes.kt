package vault.local

// Photo pagination: exact aspect-preserving rects in points.
data class PdfRect(val x: Double, val y: Double, val w: Double, val h: Double)

data class PdfPoint(val x: Double, val y: Double)

enum class PdfTone(val rgb: Int) {
    PINE(0x123D35), INK(0x28332F), MUTED(0x59665E), PAPER(0xF4F3EE),
    SAGE(0xEDF1EB), RULE(0xD8DDD5), WHITE(0xFFFFFF),
}

enum class PdfAlign { LEFT, CENTER, RIGHT }

sealed interface PageItem {
    data class Fill(val rect: PdfRect, val tone: PdfTone) : PageItem
    data class Path(val points: List<PdfPoint>, val tone: PdfTone, val strokeWidth: Double? = null) : PageItem
    data class Photo(val entry: PdfPhotoEntry, val rect: PdfRect) : PageItem
    data class Text(
        val kind: TextKind, val text: String, val rect: PdfRect? = null,
        val tone: PdfTone = kind.tone, val align: PdfAlign = PdfAlign.LEFT,
    ) : PageItem
    // Semantic audit markers; their visible text is positioned separately.
    data class Finding(val entry: PdfFindingEntry) : PageItem
    data class HashRow(val label: String, val hex: String) : PageItem
    // QR identifies Report/Manifest/format for local import. It never
    // certifies bytes on its own; the detached receipt does that.
    data class Qr(val content: String, val rect: PdfRect? = null) : PageItem
}

class RenderCancelledException : IllegalStateException("render cancelled")

// Drawing port: all photo bytes are normalized from verified originals.
// Native font metrics paginate before drawing; cancellation is cooperative.
interface PdfTextMetrics {
    fun measureText(text: String, kind: TextKind, width: Double): List<String> =
        measuredPdfLines(text, width) { it.length * kind.size * 0.55 }
    fun lineHeight(kind: TextKind): Double = kind.size * 1.2
}

interface PlanRenderer : PdfTextMetrics {
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

enum class PdfFontWeight { REGULAR, MEDIUM, SEMIBOLD }

enum class TextKind(
    val size: Double, val tone: PdfTone = PdfTone.PINE, val weight: PdfFontWeight = PdfFontWeight.REGULAR,
) {
    COVER_TITLE(44.0, weight = PdfFontWeight.MEDIUM),
    COVER_BRAND(30.0, weight = PdfFontWeight.SEMIBOLD),
    TITLE(PDF_TITLE_SIZE, weight = PdfFontWeight.SEMIBOLD),
    COUNTER(28.0, weight = PdfFontWeight.MEDIUM),
    HEADING(PDF_HEADING_SIZE, weight = PdfFontWeight.SEMIBOLD),
    VALUE(18.0, weight = PdfFontWeight.MEDIUM),
    ADDRESS(17.0, PdfTone.INK),
    BRAND(15.0, weight = PdfFontWeight.SEMIBOLD),
    BODY(PDF_BODY_SIZE, PdfTone.INK),
    CAPTION(10.5, PdfTone.INK),
    SMALL(PDF_SMALL_SIZE, PdfTone.MUTED),
    LABEL(8.5, weight = PdfFontWeight.MEDIUM),
    FOOTER(8.0, PdfTone.MUTED),
}

data class RenderPage(
    val number: Int, val header: String, val items: List<PageItem>,
    val outlineTitle: String? = null, val isCover: Boolean = false,
    val chrome: List<PageItem> = emptyList(),
)

data class ImageDims(val width: Int, val height: Int)

// Dimensions without full decode (bounded memory); null when undecodable.
expect fun imageDimensions(bytes: ByteArray): ImageDims?

// Display-only seal timestamp: "01/10/2026 às 12:10" in the device
// timezone. The raw ISO stays in the model and manifest; unparsable
// input falls back to the raw string so the PDF never breaks.
expect fun formatSealedAtDevice(iso: String): String

fun contentWidthPt(): Double = PDF_PAGE_WIDTH_PT - PDF_MARGIN_LEFT_PT - PDF_MARGIN_RIGHT_PT

fun bottomLimitPt(): Double = PDF_PAGE_HEIGHT_PT - PDF_MARGIN_BOTTOM_PT

