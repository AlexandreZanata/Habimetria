package vault.local

import android.graphics.BitmapFactory

actual fun imageDimensions(bytes: ByteArray): ImageDims? {
    if (bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    return orientedDimensions(options.outWidth, options.outHeight, androidImageOrientation(bytes))
}

actual fun formatSealedAtDevice(iso: String): String = try {
    val zoned = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(zoned)
} catch (_: Exception) {
    iso
}
