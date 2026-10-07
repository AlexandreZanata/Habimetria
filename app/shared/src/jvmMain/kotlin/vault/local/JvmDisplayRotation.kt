package vault.local

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun rotateDisplayBytes(bytes: ByteArray, degrees: Int): ByteArray {
    requireDisplayRotation(degrees)
    if (degrees == 0) return bytes
    val source: BufferedImage = ImageIO.read(ByteArrayInputStream(bytes))
        ?: throw LocalStoreException("undecodable image")
    val rotated = when (degrees) {
        90 -> BufferedImage(source.height, source.width, BufferedImage.TYPE_INT_RGB).also { out ->
            for (y in 0 until source.height) for (x in 0 until source.width) {
                out.setRGB(source.height - 1 - y, x, source.getRGB(x, y))
            }
        }
        180 -> BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB).also { out ->
            for (y in 0 until source.height) for (x in 0 until source.width) {
                out.setRGB(source.width - 1 - x, source.height - 1 - y, source.getRGB(x, y))
            }
        }
        270 -> BufferedImage(source.height, source.width, BufferedImage.TYPE_INT_RGB).also { out ->
            for (y in 0 until source.height) for (x in 0 until source.width) {
                out.setRGB(y, source.width - 1 - x, source.getRGB(x, y))
            }
        }
        else -> throw LocalValidationException("display rotation must be 0/90/180/270")
    }
    val out = ByteArrayOutputStream()
    if (!ImageIO.write(rotated, "jpg", out)) throw LocalStoreException("encode failed")
    return out.toByteArray()
}
