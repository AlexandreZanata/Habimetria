package vault.local

import android.graphics.BitmapFactory

actual fun imageDimensions(bytes: ByteArray): ImageDims? {
    if (bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    return ImageDims(options.outWidth, options.outHeight)
}
