package vault.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

actual fun rotateDisplayBytes(bytes: ByteArray, degrees: Int): ByteArray {
    requireDisplayRotation(degrees)
    if (degrees == 0) return bytes
    val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw LocalStoreException("undecodable image")
    try {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        try {
            val out = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, 85, out)
            return out.toByteArray()
        } finally {
            if (rotated !== source) rotated.recycle()
        }
    } finally {
        source.recycle()
    }
}
