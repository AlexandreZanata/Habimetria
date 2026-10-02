package vault.local

import android.graphics.BitmapFactory

actual fun imageInfo(bytes: ByteArray): PhotoDisplay? {
    if (bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val orientation = androidImageOrientation(bytes)
    val displayed = orientedDimensions(options.outWidth, options.outHeight, orientation)
    return PhotoDisplay(options.outWidth, options.outHeight, displayed.width, displayed.height, orientation)
}

actual fun formatSealedAtDevice(iso: String): String = try {
    val zoned = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(zoned)
} catch (_: Exception) {
    iso
}

actual fun imageDimensions(bytes: ByteArray): ImageDims? = imageInfo(bytes)?.dimensions()
