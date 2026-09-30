package vault.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream

actual fun platformScaler(): ImageScaler = AndroidScaler()

class AndroidScaler : ImageScaler {
    override fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray {
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw LocalStoreException("undecodable image")
        try {
            bitmap = applyExifOrientation(bytes, bitmap)
            val w = bitmap.width
            val h = bitmap.height
            val longest = maxOf(w, h)
            if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(
                    bitmap, (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1), true,
                )
                if (scaled != bitmap) bitmap.recycle()
                bitmap = scaled
            }
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            return out.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    // Derivatives follow the original's EXIF orientation so grids match
    // the captured scene; the stored original bytes stay untouched.
    private fun applyExifOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSPOSE -> 90f
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }
}
