package vault.local

import java.awt.image.BufferedImage

internal fun orientJvmImage(source: BufferedImage, orientation: Int): BufferedImage {
    val dims = orientedDimensions(source.width, source.height, orientation)
    val output = BufferedImage(dims.width, dims.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until source.height) for (x in 0 until source.width) {
        val (dx, dy) = when (orientation) {
            2 -> source.width - 1 - x to y
            3 -> source.width - 1 - x to source.height - 1 - y
            4 -> x to source.height - 1 - y
            5 -> y to x
            6 -> source.height - 1 - y to x
            7 -> source.height - 1 - y to source.width - 1 - x
            8 -> y to source.width - 1 - x
            else -> x to y
        }
        output.setRGB(dx, dy, source.getRGB(x, y))
    }
    return output
}
