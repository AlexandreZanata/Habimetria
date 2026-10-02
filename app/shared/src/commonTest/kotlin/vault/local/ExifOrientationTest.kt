package vault.local

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExifOrientationTest {
    private fun jpeg(orientation: Int, little: Boolean): ByteArray {
        val tiff = if (little) byteArrayOf(
            73, 73, 42, 0, 8, 0, 0, 0, 1, 0, 18, 1, 3, 0,
            1, 0, 0, 0, orientation.toByte(), 0, 0, 0, 0, 0, 0, 0,
        ) else byteArrayOf(
            77, 77, 0, 42, 0, 0, 0, 8, 0, 1, 1, 18, 0, 3,
            0, 0, 0, 1, 0, orientation.toByte(), 0, 0, 0, 0, 0, 0,
        )
        val segment = byteArrayOf(69, 120, 105, 102, 0, 0) + tiff
        return byteArrayOf(-1, -40, -1, -31, 0, (segment.size + 2).toByte()) + segment
    }

    @Test fun bothByteOrdersAndAllEightOrientations() {
        for (value in 1..8) for (little in listOf(true, false)) {
            assertEquals(value, jpegExifOrientation(jpeg(value, little)))
            val expected = if (value in 5..8) ImageDims(300, 800) else ImageDims(800, 300)
            assertEquals(expected, orientedDimensions(800, 300, value))
        }
    }

    @Test fun mirrorsHaveDistinctTransformsAndMalformedMetadataIsSafe() {
        assertContentEquals(floatArrayOf(-1f, 0f, 0f, 1f), orientationMatrix(2))
        assertContentEquals(floatArrayOf(0f, 1f, 1f, 0f), orientationMatrix(5))
        assertContentEquals(floatArrayOf(0f, -1f, -1f, 0f), orientationMatrix(7))
        assertEquals(1, jpegExifOrientation(jpeg(9, true)))
        for (length in 0 until jpeg(6, true).size) {
            assertEquals(1, jpegExifOrientation(jpeg(6, true).copyOf(length)))
        }
    }
}
