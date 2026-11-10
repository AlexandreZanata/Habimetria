package vault.local

import vault.local.PdfTone.*

internal fun pdfLine(x1: Double, y1: Double, x2: Double, y2: Double, tone: PdfTone): PageItem =
    PageItem.Path(listOf(PdfPoint(x1, y1), PdfPoint(x2, y2)), tone, 0.6)

private fun pdfMark(x: Double, y: Double, width: Double): PageItem {
    val outline = listOf(
        0.0 to 0.0, 0.252 to 0.0, 0.252 to 0.33, 0.748 to 0.33,
        0.748 to 0.0, 1.0 to 0.0, 1.0 to 0.88, 0.748 to 0.88,
        0.748 to 0.595, 0.584 to 0.595, 0.584 to 0.505, 0.416 to 0.505,
        0.416 to 0.595, 0.252 to 0.595, 0.252 to 0.88, 0.0 to 0.88,
    )
    return PageItem.Path(outline.map { (dx, dy) -> PdfPoint(x + width * dx, y + width * dy) }, PINE)
}

internal fun PdfLayoutBuilder.markerAll(items: List<PageItem>) { items.forEach(::marker) }

// Added after pagination so page counts and physical index references agree.
internal fun RenderPage.withChrome(model: PdfModel, metrics: PdfTextMetrics, total: Int): RenderPage {
    val chrome = PdfLayoutBuilder(metrics, header)
    if (isCover) {
        chrome.marker(pdfMark(pdfBody.x, 80.0, 39.0))
        chrome.put("habimetria", TextKind.COVER_BRAND, 71.0, PdfColumn(134.0, 230.0))
        chrome.put("Cada detalhe, documentado.", TextKind.CAPTION, 121.0, tone = PINE)
        chrome.put("RELATÓRIO LOCAL", TextKind.LABEL, 92.0, pdfBody.copy(align = PdfAlign.RIGHT))
        chrome.rule(142.0, PINE)
    } else {
        chrome.marker(pdfMark(pdfBody.x, 39.0, 20.0))
        chrome.put("habimetria", TextKind.BRAND, 34.0, PdfColumn(115.0, 160.0))
        chrome.put(model.inspectionLabel.uppercase(), TextKind.FOOTER, 41.0, pdfBody.copy(align = PdfAlign.RIGHT))
        chrome.rule(73.0, PINE)
    }
    chrome.rule(789.0, if (isCover) PINE else RULE)
    chrome.put("RELATÓRIO / $header", TextKind.FOOTER, 799.0, PdfColumn(pdfBody.x, 360.0))
    chrome.put("${number.pdfNumber()} / ${total.pdfNumber()}", TextKind.FOOTER, 799.0,
        PdfColumn(453.0, pdfRight - 453, PdfAlign.RIGHT), PINE)
    chrome.flush()
    return copy(chrome = chrome.pages.single().items)
}
