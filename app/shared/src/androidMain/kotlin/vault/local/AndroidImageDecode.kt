package vault.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

internal fun androidImageOrientation(bytes: ByteArray): Int = try {
    ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        .takeIf { it in 1..8 } ?: 1
} catch (_: Exception) {
    1
}

internal fun decodeOrientedAndroid(bytes: ByteArray, maxEdge: Int): Bitmap {
    require(maxEdge > 0)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "undecodable image" }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) sample *= 2
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample })
        ?: throw LocalStoreException("undecodable image")
    val transform = orientationMatrix(androidImageOrientation(bytes))
    val matrix = Matrix().apply {
        setValues(floatArrayOf(transform[0], transform[1], 0f, transform[2], transform[3], 0f, 0f, 0f, 1f))
    }
    return try {
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            .also { if (it !== original) original.recycle() }
    } catch (e: Exception) {
        original.recycle()
        throw e
    }
}
