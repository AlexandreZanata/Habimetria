package vault.local

// Logical navigation outline: section titles with physical page numbers.
// Renderers expose native bookmarks only where the platform writer
// supports outlines. Android PdfDocument and the current iOS writer have
// no outline API, so the printed contents page stays the portable
// navigation surface; this model keeps page references exact for any
// future writer that can embed them.
data class PdfOutlineEntry(val title: String, val pageNumber: Int)

fun outlineForPages(pages: List<RenderPage>): List<PdfOutlineEntry> {
    val sections = mutableListOf<PdfOutlineEntry>()
    pages.forEach { page ->
        val first = page.items.filterIsInstance<PageItem.Text>().firstOrNull()
        val title = page.outlineTitle ?: first?.takeIf {
            it.kind == TextKind.TITLE || it.kind == TextKind.HEADING
        }?.text
        if (!title.isNullOrBlank() && sections.lastOrNull()?.title != title) {
            sections.add(PdfOutlineEntry(title, page.number))
        }
    }
    return sections
}

// Native bookmark support by writer. Both current writers draw contents
// without embedded outlines; return false so callers never claim them.
fun nativeBookmarksSupported(renderer: String): Boolean = when (renderer) {
    "android-pdf-document" -> false
    "ios-uikit-pdf" -> false
    else -> false
}
