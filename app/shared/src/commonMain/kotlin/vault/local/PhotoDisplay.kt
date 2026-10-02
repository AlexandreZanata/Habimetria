package vault.local

import kotlinx.serialization.Serializable

// Dimensions and EXIF transform read from verified originals at seal time.
@Serializable
data class PhotoDisplay(val encodedWidth: Int, val encodedHeight: Int, val displayWidth: Int,
    val displayHeight: Int, val orientation: Int) {
    fun dimensions(): ImageDims = ImageDims(displayWidth, displayHeight)
}
expect fun imageInfo(bytes: ByteArray): PhotoDisplay?
