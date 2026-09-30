package vault.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveTest {

    @Test
    fun breakpointsMatchMaterialClasses() {
        assertEquals(WindowClass.COMPACT, windowClassFor(0))
        assertEquals(WindowClass.COMPACT, windowClassFor(599))
        assertEquals(WindowClass.MEDIUM, windowClassFor(600))
        assertEquals(WindowClass.MEDIUM, windowClassFor(839))
        assertEquals(WindowClass.EXPANDED, windowClassFor(840))
        assertEquals(WindowClass.EXPANDED, windowClassFor(1280))
    }

    @Test
    fun contentCapsAndColumns() {
        assertEquals(-1, maxContentWidthDp(WindowClass.COMPACT))
        assertEquals(720, maxContentWidthDp(WindowClass.MEDIUM))
        assertEquals(960, maxContentWidthDp(WindowClass.EXPANDED))
        assertEquals(1, roomsColumns(WindowClass.COMPACT))
        assertEquals(2, roomsColumns(WindowClass.MEDIUM))
        assertEquals(3, roomsColumns(WindowClass.EXPANDED))
    }

    @Test
    fun sideBySideOnlyWhenWide() {
        assertFalse(useSideBySide(WindowClass.COMPACT))
        assertTrue(useSideBySide(WindowClass.MEDIUM))
        assertTrue(useSideBySide(WindowClass.EXPANDED))
    }
}
