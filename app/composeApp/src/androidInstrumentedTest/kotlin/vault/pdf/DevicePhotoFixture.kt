package vault.pdf

import android.graphics.*
import java.io.ByteArrayOutputStream
import vault.local.*

// Four asymmetric corners make every mirror/rotation observable.
internal fun devicePhoto(orientation: Int, portrait: Boolean = false): ByteArray {
    val width = if (portrait) 800 else 1200
    val height = if (portrait) 1200 else 800
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW).forEachIndexed { index, color ->
        paint.color = color
        val x = (index % 2) * width / 2f; val y = (index / 2) * height / 2f
        canvas.drawRect(x, y, x + width / 2f, y + height / 2f, paint)
    }
    paint.color = Color.BLACK; paint.textSize = 70f
    canvas.drawText("FOTO SINTÉTICA · EXIF $orientation", 35f, height / 2f, paint)
    val bytes = ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it); it.toByteArray() }
    bitmap.recycle()
    val exif = byteArrayOf(0x45,0x78,0x69,0x66,0,0,0x49,0x49,0x2a,0,8,0,0,0,1,0,
        0x12,1,3,0,1,0,0,0,orientation.toByte(),0,0,0,0,0,0,0)
    val length = exif.size + 2
    return bytes.take(2).toByteArray() + byteArrayOf(0xff.toByte(),0xe1.toByte(),(length shr 8).toByte(),length.toByte()) + exif + bytes.drop(2).toByteArray()
}

internal fun checkDeviceOrientations() {
    val expected = listOf(listOf(0,1,2,3), listOf(1,0,3,2), listOf(3,2,1,0), listOf(2,3,0,1),
        listOf(0,2,1,3), listOf(2,0,3,1), listOf(3,1,2,0), listOf(1,3,0,2))
    val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
    (1..8).forEach { orientation ->
        val bytes = devicePhoto(orientation)
        check(jpegExifOrientation(bytes) == orientation)
        val dims = requireNotNull(imageDimensions(bytes))
        check(dims == if (orientation >= 5) ImageDims(800,1200) else ImageDims(1200,800))
        val normalized = decodeOrientedAndroid(bytes, 1600)
        val positions = listOf(0.2 to 0.2, 0.8 to 0.2, 0.2 to 0.8, 0.8 to 0.8)
        positions.forEachIndexed { i, (x,y) ->
            val actual = normalized.getPixel((x * normalized.width).toInt(), (y * normalized.height).toInt())
            val wanted = colors[expected[orientation-1][i]]
            check(kotlin.math.abs(Color.red(actual)-Color.red(wanted)) < 20 &&
                kotlin.math.abs(Color.green(actual)-Color.green(wanted)) < 20 &&
                kotlin.math.abs(Color.blue(actual)-Color.blue(wanted)) < 20) { "wrong EXIF $orientation corner $i" }
        }
        normalized.recycle()
    }
}
