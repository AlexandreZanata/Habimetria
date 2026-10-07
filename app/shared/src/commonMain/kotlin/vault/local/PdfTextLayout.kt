package vault.local

// Breaks using the actual drawing font, including long unspaced words.
// No content limit; newline and Unicode surrogate pairs remain meaningful.
fun measuredPdfLines(text: String, maxWidth: Double, widthOf: (String) -> Double): List<String> {
    require(maxWidth > 0)
    val lines = mutableListOf<String>()
    for (paragraph in text.replace("\r\n", "\n").split('\n')) {
        var rest = paragraph
        if (rest.isEmpty()) lines.add("")
        while (rest.isNotEmpty()) {
            if (widthOf(rest) <= maxWidth) { lines.add(rest); break }
            var low = 1
            var high = rest.length
            while (low < high) {
                val middle = (low + high + 1) / 2
                if (widthOf(rest.substring(0, middle)) <= maxWidth) low = middle else high = middle - 1
            }
            var end = low
            if (end < rest.length && rest[end - 1].isHighSurrogate() && rest[end].isLowSurrogate()) end--
            if (end == 0) end = if (rest.length > 1 && rest[0].isHighSurrogate()) 2 else 1
            val space = rest.substring(0, end).indexOfLast { it.isWhitespace() }
            if (space > 0) end = space + 1
            lines.add(rest.substring(0, end).trimEnd())
            rest = rest.substring(end)
        }
    }
    return lines
}

fun textSize(kind: TextKind): Double = when (kind) {
    TextKind.TITLE -> PDF_TITLE_SIZE
    TextKind.HEADING -> PDF_HEADING_SIZE
    TextKind.BODY -> PDF_BODY_SIZE
    TextKind.SMALL -> PDF_SMALL_SIZE
}

internal class PdfLayoutBuilder(private val renderer: PdfTextMetrics, private val reportId: String) {
    val pages = mutableListOf<RenderPage>()
    private var items = mutableListOf<PageItem>()
    var y = PDF_MARGIN_TOP_PT
        private set
    val currentNumber: Int get() = pages.size + 1

    fun ensure(height: Double) { if (y + height > bottomLimitPt()) flush() }
    fun gap(height: Double = 8.0) { ensure(height); y += height }
    fun marker(item: PageItem) { items.add(item) }
    fun text(text: String, kind: TextKind = TextKind.BODY, gapAfter: Double = 6.0) {
        for (line in renderer.measureText(text, kind, contentWidthPt())) {
            val height = renderer.lineHeight(kind)
            ensure(height)
            items.add(PageItem.Text(kind, line, PdfRect(PDF_MARGIN_LEFT_PT, y, contentWidthPt(), height)))
            y += height
        }
        gap(gapAfter)
    }
    fun photo(entry: PdfPhotoEntry, dims: ImageDims) {
        require(dims.width > 0 && dims.height > 0) { "invalid image dimensions" }
        val aspect = dims.width.toDouble() / dims.height
        val height = minOf(contentWidthPt() / aspect, PDF_MAX_PHOTO_HEIGHT_PT)
        val width = height * aspect
        val captions = listOf(entry.caption, entry.claimLine.orEmpty())
        val captionHeight = captions.sumOf { renderer.measureText(it, TextKind.SMALL, contentWidthPt()).size } *
            renderer.lineHeight(TextKind.SMALL) + 12.0
        // Reserve image + its reference. Long claim blocks may continue afterward.
        ensure(height + minOf(captionHeight, 100.0))
        items.add(PageItem.Photo(entry, PdfRect(PDF_MARGIN_LEFT_PT + (contentWidthPt() - width) / 2, y, width, height)))
        y += height + 6.0
        text(entry.caption, TextKind.SMALL, 2.0)
        entry.claimLine?.let { text(it, TextKind.SMALL) }
        gap(6.0)
    }
    fun qr(content: String) {
        ensure(108.0)
        items.add(PageItem.Qr(content, PdfRect(PDF_MARGIN_LEFT_PT, y, 100.0, 100.0)))
        y += 108.0
    }
    fun flush() {
        if (items.isNotEmpty()) pages.add(RenderPage(pages.size + 1, reportId, items.toList()))
        items = mutableListOf()
        y = PDF_MARGIN_TOP_PT
    }
}
