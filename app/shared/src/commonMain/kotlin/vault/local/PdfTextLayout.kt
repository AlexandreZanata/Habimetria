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

internal data class PdfColumn(val x: Double, val width: Double, val align: PdfAlign = PdfAlign.LEFT)
internal val pdfBody = PdfColumn(PDF_MARGIN_LEFT_PT, contentWidthPt())
internal val pdfRight = PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT

internal class PdfLayoutBuilder(private val renderer: PdfTextMetrics, private val reportId: String) {
    val pages = mutableListOf<RenderPage>()
    private var items = mutableListOf<PageItem>()
    private var outlineTitle: String? = null
    private var continuation: String? = null
    var y = 99.0
        private set
    val currentNumber: Int get() = pages.size + 1

    fun heading(label: String, title: String, subtitle: String? = null, column: PdfColumn = pdfBody) {
        outlineTitle = title
        text(label, TextKind.LABEL, 10.0)
        text(title, TextKind.TITLE, 8.0, column)
        subtitle?.let { text(it, gapAfter = 12.0) }
        continuation = label
    }

    fun ensure(height: Double) {
        if (y + height <= bottomLimitPt()) return
        val label = continuation
        flush()
        if (label != null) {
            continuation = label
            text("$label · CONTINUAÇÃO", TextKind.LABEL, 16.0)
        }
    }

    fun spaceTo(top: Double) { y = maxOf(y, top) }
    fun gap(height: Double = 8.0) { y += height }
    fun marker(item: PageItem) { items.add(item) }

    fun lines(text: String, kind: TextKind, column: PdfColumn = pdfBody): List<String> =
        renderer.measureText(text, kind, column.width)

    fun height(kind: TextKind): Double = renderer.lineHeight(kind)

    fun put(line: String, kind: TextKind, top: Double, column: PdfColumn = pdfBody, tone: PdfTone = kind.tone) {
        marker(PageItem.Text(kind, line, PdfRect(column.x, top, column.width, height(kind)), tone, column.align))
    }

    fun cell(text: String, kind: TextKind, top: Double, column: PdfColumn = pdfBody, tone: PdfTone = kind.tone) {
        lines(text, kind, column).forEachIndexed { i, line -> put(line, kind, top + i * height(kind), column, tone) }
    }

    fun text(text: String, kind: TextKind = TextKind.BODY, gapAfter: Double = 6.0, column: PdfColumn = pdfBody) {
        for (line in lines(text, kind, column)) {
            ensure(height(kind))
            put(line, kind, y, column)
            y += height(kind)
        }
        gap(gapAfter)
    }

    fun fill(top: Double, height: Double, tone: PdfTone, column: PdfColumn = pdfBody) {
        marker(PageItem.Fill(PdfRect(column.x, top, column.width, height), tone))
    }

    fun rule(top: Double = y, tone: PdfTone = PdfTone.RULE) {
        marker(PageItem.Path(listOf(PdfPoint(pdfBody.x, top), PdfPoint(pdfRight, top)), tone, 0.6))
    }

    fun panel(label: String, content: String, tone: PdfTone = PdfTone.SAGE, accent: Boolean = true) {
        val column = PdfColumn(pdfBody.x + 16, pdfBody.width - 32)
        val body = lines(content, TextKind.CAPTION, column)
        val lineHeight = height(TextKind.CAPTION)
        val labels = lines(label, TextKind.LABEL, column)
        val padding = 30.0 + labels.size * height(TextKind.LABEL)
        val available = bottomLimitPt() - y
        val entire = padding + lineHeight * body.size
        if (entire > available) ensure(minOf(entire, padding + lineHeight * 3))
        var start = 0
        while (start < body.size) {
            ensure(padding + lineHeight)
            val count = minOf(body.size - start, ((bottomLimitPt() - y - padding) / lineHeight).toInt())
            val panelHeight = padding + lineHeight * count
            fill(y, panelHeight, tone)
            if (accent) fill(y, panelHeight, PdfTone.PINE, PdfColumn(pdfBody.x, 3.0))
            labels.forEachIndexed { i, line -> put(line, TextKind.LABEL, y + 10 + i * height(TextKind.LABEL), column) }
            body.subList(start, start + count).forEachIndexed { i, line ->
                put(line, TextKind.CAPTION, y + padding - 11 + i * lineHeight, column)
            }
            y += panelHeight + 10
            start += count
        }
    }

    fun photo(entry: PdfPhotoEntry, dims: ImageDims, maxHeight: Double) {
        require(dims.width > 0 && dims.height > 0) { "invalid image dimensions" }
        val aspect = dims.width.toDouble() / dims.height
        val height = minOf(pdfBody.width / aspect, maxHeight)
        val width = height * aspect
        ensure(height + 6 + lines(entry.caption, TextKind.CAPTION).size * height(TextKind.CAPTION))
        // For extreme panoramas or narrow photos, full metadata still appears below the image.
        val stamp = if (width >= 24) wrapStampLines(entry.stampLines, width - 8, renderer::measureText) else emptyList()
        val fits = stamp.size * (height(TextKind.SMALL) + 2) + 8 <= height
        val slot = PdfRect(pdfBody.x + (pdfBody.width - width) / 2, y, width, height)
        marker(PageItem.Photo(entry.copy(stampLines = if (fits) stamp else emptyList()), slot))
        y += height + 6
        text(entry.caption, TextKind.CAPTION, 2.0)
        entry.claimLine?.let {
            ensure(35.0)
            text("DADOS DO REGISTRO", TextKind.LABEL, 2.0)
            text(it, TextKind.SMALL, 12.0)
        }
    }

    fun flush() {
        if (items.isNotEmpty()) pages.add(RenderPage(currentNumber, reportId, items.toList(), outlineTitle))
        items = mutableListOf()
        y = 99.0
        outlineTitle = null
        continuation = null
    }
}
