package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath

// Derivative images (thumbnail + preview) decoded from the immutable
// original at capture time. The original is never modified, rotated or
// recompressed by this process; derivatives are recreatable and carry
// no trust weight.
const val THUMB_MAX_EDGE = 320
const val PREVIEW_MAX_EDGE = 1600

interface ImageScaler {
    // Returns JPEG bytes scaled to fit maxEdge, aspect preserved.
    fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray
}

expect fun platformScaler(): ImageScaler

class DerivativeStore(
    private val fs: FileSystem,
    private val filesDir: String,
    private val scaler: ImageScaler,
) {
    private fun dir(): String = "$filesDir/vault/derivatives"

    fun thumbPath(evidenceId: String): String = "${dir()}/${evidenceId}_thumb.jpg"

    fun previewPath(evidenceId: String): String = "${dir()}/${evidenceId}_preview.jpg"

    // Idempotent: existing derivatives are left untouched.
    fun ensureDerivatives(evidenceId: String, original: ByteArray) {
        fs.createDirectories(dir().toPath())
        val thumb = thumbPath(evidenceId).toPath()
        val preview = previewPath(evidenceId).toPath()
        if (!fs.exists(thumb)) {
            fs.write(thumb) { write(scaler.scaleJpeg(original, THUMB_MAX_EDGE)) }
        }
        if (!fs.exists(preview)) {
            fs.write(preview) { write(scaler.scaleJpeg(original, PREVIEW_MAX_EDGE)) }
        }
    }

    fun hasDerivatives(evidenceId: String): Boolean =
        fs.exists(thumbPath(evidenceId).toPath()) && fs.exists(previewPath(evidenceId).toPath())

    // Grids decode the small thumb; rows captured before derivatives
    // existed fall back to the original file.
    fun displayPath(evidenceId: String, originalPath: String): String =
        if (fs.exists(thumbPath(evidenceId).toPath())) thumbPath(evidenceId) else originalPath

    // Lazy repair for views: regenerates a missing thumb from the
    // original on demand. Codec failures keep the original as the
    // source of truth instead of failing the view.
    fun ensureForDisplay(evidenceId: String, originalPath: String): String {
        if (!fs.exists(thumbPath(evidenceId).toPath()) && fs.exists(originalPath.toPath())) {
            try {
                ensureDerivatives(evidenceId, fs.read(originalPath.toPath()) { readByteArray() })
            } catch (_: Exception) {
                // Fall through to the original below.
            }
        }
        return displayPath(evidenceId, originalPath)
    }
}
