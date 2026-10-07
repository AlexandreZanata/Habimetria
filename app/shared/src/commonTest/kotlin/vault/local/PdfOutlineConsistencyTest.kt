package vault.local

import kotlin.test.Test
import kotlin.test.assertTrue

// Portable navigation: the printed contents page carries exact physical page
// references because the current Android/iOS writers expose no native
// outline API. The logical outline must agree with paginated pages.
class PdfOutlineConsistencyTest {
    @Test
    fun paginatedPagesExposeMatchingOutline() {
        val pages = listOf(
            RenderPage(1, "Cover", listOf(PageItem.Text(TextKind.TITLE, "Report"))),
            RenderPage(2, "Index", listOf(PageItem.Text(TextKind.HEADING, "SUMMARY"))),
            RenderPage(3, "Room", listOf(PageItem.Text(TextKind.HEADING, "2.1 Sala"))),
        )
        val outline = outlineForPages(pages)
        assertTrue(outline.isNotEmpty())
        outline.forEach { entry ->
            assertTrue(pages.any { it.number == entry.pageNumber })
        }
        assertTrue(!nativeBookmarksSupported("android-pdf-document"))
        assertTrue(!nativeBookmarksSupported("ios-uikit-pdf"))
    }
}
