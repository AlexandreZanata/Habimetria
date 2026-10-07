package vault.local

// JPEG EXIF orientation for the JVM verifier/test codec. Native adapters
// use their metadata codecs as well, so supported HEIF stays supported.
fun jpegExifOrientation(bytes: ByteArray): Int {
    fun u8(at: Int) = bytes[at].toInt() and 255
    if (bytes.size < 4 || u8(0) != 255 || u8(1) != 216) return 1
    var pos = 2
    while (pos + 4 <= bytes.size) {
        if (u8(pos) != 255) return 1
        val marker = u8(pos + 1)
        if (marker == 218 || marker == 217) return 1
        val length = u8(pos + 2) * 256 + u8(pos + 3)
        val end = pos + 2 + length
        if (length < 2 || end > bytes.size) return 1
        val header = pos + 4
        if (marker == 225 && length >= 16 &&
            bytes.copyOfRange(header, header + 6).contentEquals(byteArrayOf(69, 120, 105, 102, 0, 0))
        ) return tiffOrientation(bytes, header + 6, end)
        pos = end
    }
    return 1
}

private fun tiffOrientation(bytes: ByteArray, base: Int, end: Int): Int = try {
    val little = bytes[base] == 73.toByte() && bytes[base + 1] == 73.toByte()
    require(little || bytes[base] == 77.toByte() && bytes[base + 1] == 77.toByte())
    fun number(offset: Long, size: Int): Long {
        require(offset >= 0 && base.toLong() + offset + size <= end)
        var result = 0L
        repeat(size) { i ->
            val shift = if (little) i * 8 else (size - 1 - i) * 8
            result = result or ((bytes[base + offset.toInt() + i].toLong() and 255) shl shift)
        }
        return result
    }
    require(number(2, 2) == 42L)
    val ifd = number(4, 4)
    val count = number(ifd, 2).toInt()
    require(count <= 4096)
    var orientation = 1
    repeat(count) { i ->
        val entry = ifd + 2 + i * 12L
        if (number(entry, 2) == 274L && number(entry + 2, 2) == 3L && number(entry + 4, 4) == 1L) {
            orientation = number(entry + 8, 2).toInt().takeIf { it in 1..8 } ?: 1
        }
    }
    orientation
} catch (_: IllegalArgumentException) {
    1
} catch (_: IndexOutOfBoundsException) {
    1
}

fun orientedDimensions(width: Int, height: Int, orientation: Int): ImageDims =
    if (orientation in 5..8) ImageDims(height, width) else ImageDims(width, height)

// Linear transform; native bitmap creation translates bounds automatically.
fun orientationMatrix(orientation: Int): FloatArray = when (orientation) {
    2 -> floatArrayOf(-1f, 0f, 0f, 1f)
    3 -> floatArrayOf(-1f, 0f, 0f, -1f)
    4 -> floatArrayOf(1f, 0f, 0f, -1f)
    5 -> floatArrayOf(0f, 1f, 1f, 0f)
    6 -> floatArrayOf(0f, -1f, 1f, 0f)
    7 -> floatArrayOf(0f, -1f, -1f, 0f)
    8 -> floatArrayOf(0f, 1f, -1f, 0f)
    else -> floatArrayOf(1f, 0f, 0f, 1f)
}
