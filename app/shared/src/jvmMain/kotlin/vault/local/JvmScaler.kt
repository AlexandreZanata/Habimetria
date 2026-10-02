package vault.local

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun platformScaler(): ImageScaler = JvmScaler()

class JvmScaler : ImageScaler {
    override fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray {
        val raw: BufferedImage = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw LocalStoreException("undecodable image")
        val source = orientJvmImage(raw, jpegExifOrientation(bytes))
        val longest = maxOf(source.width, source.height)
        val target = if (longest > maxEdge) {
            val scale = maxEdge.toDouble() / longest
            val scaled = BufferedImage(
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                BufferedImage.TYPE_INT_RGB,
            )
            val graphics = scaled.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(source, 0, 0, scaled.width, scaled.height, null)
            graphics.dispose()
            scaled
        } else {
            source
        }
        val out = ByteArrayOutputStream()
        if (!ImageIO.write(target, "jpg", out)) throw LocalStoreException("encode failed")
        return out.toByteArray()
    }
}
