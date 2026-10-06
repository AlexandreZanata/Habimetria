package vault.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DisplayCorrectionTest {
    @Test
    fun onlyQuarterTurnsAreAccepted() {
        assertTrue(validDisplayRotation(0))
        assertTrue(validDisplayRotation(90))
        assertTrue(validDisplayRotation(180))
        assertTrue(validDisplayRotation(270))
        assertFailsWith<IllegalArgumentException> { requireDisplayRotation(45) }
        assertFailsWith<IllegalArgumentException> { requireDisplayRotation(-90) }
    }

    @Test
    fun quarterTurnSwapsDimensions() {
        val base = ImageDims(800, 600)
        assertEquals(ImageDims(800, 600), correctedDisplayDims(base, 0))
        assertEquals(ImageDims(600, 800), correctedDisplayDims(base, 90))
        assertEquals(ImageDims(800, 600), correctedDisplayDims(base, 180))
        assertEquals(ImageDims(600, 800), correctedDisplayDims(base, 270))
    }

    @Test
    fun photoDisplayKeepsEncodedButCorrectsDisplay() {
        val base = PhotoDisplay(800, 600, 800, 600, 1)
        val corrected = correctedPhotoDisplay(base, 90)
        assertEquals(800, corrected.encodedWidth)
        assertEquals(600, corrected.encodedHeight)
        assertEquals(600, corrected.displayWidth)
        assertEquals(800, corrected.displayHeight)
        assertEquals(1, corrected.orientation)
    }
}
