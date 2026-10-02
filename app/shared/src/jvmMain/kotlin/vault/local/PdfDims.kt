package vault.local

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual fun imageInfo(bytes: ByteArray): PhotoDisplay? {
    if (bytes.isEmpty()) return null
    return try {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes))?.use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = stream
                val width = reader.getWidth(0); val height = reader.getHeight(0); val orientation = jpegExifOrientation(bytes)
                val displayed = orientedDimensions(width, height, orientation)
                PhotoDisplay(width, height, displayed.width, displayed.height, orientation)
            } finally {
                reader.dispose()
            }
        }
    } catch (_: Exception) {
        null
    }
}

actual fun formatSealedAtDevice(iso: String): String = try {
    val zoned = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(zoned)
} catch (_: Exception) {
    iso
}

actual fun imageDimensions(bytes: ByteArray): ImageDims? = imageInfo(bytes)?.dimensions()
