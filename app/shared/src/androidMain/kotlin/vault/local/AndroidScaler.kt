package vault.local

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

actual fun platformScaler(): ImageScaler = AndroidScaler()

class AndroidScaler : ImageScaler {
    override fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray {
        var bitmap = decodeOrientedAndroid(bytes, maxEdge)
        try {
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

}
