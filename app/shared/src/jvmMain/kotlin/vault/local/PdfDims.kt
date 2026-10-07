package vault.local

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual fun imageDimensions(bytes: ByteArray): ImageDims? {
    if (bytes.isEmpty()) return null
    return try {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes))?.use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = stream
                ImageDims(reader.getWidth(0), reader.getHeight(0))
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
