package vault.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfOutlineTest {
    @Test
    fun outlineKeepsPhysicalPageReferences() {
        val pages = listOf(
            RenderPage(1, "Cover", listOf(PageItem.Text(TextKind.TITLE, "Report"))),
            RenderPage(2, "Index", listOf(PageItem.Text(TextKind.HEADING, "SUMMARY"))),
            RenderPage(3, "Room", listOf(PageItem.Text(TextKind.HEADING, "2.1 Sala"))),
        )
        val outline = outlineForPages(pages)
        assertEquals(3, outline.size)
        assertEquals(PdfOutlineEntry("Report", 1), outline[0])
        assertEquals(PdfOutlineEntry("2.1 Sala", 3), outline[2])
    }

    @Test
    fun currentWritersExposeContentsWithoutEmbeddedBookmarks() {
        assertFalse(nativeBookmarksSupported("android-pdf-document"))
        assertFalse(nativeBookmarksSupported("ios-uikit-pdf"))
        assertFalse(nativeBookmarksSupported("unknown"))
        assertTrue(outlineForPages(emptyList()).isEmpty())
    }
}
