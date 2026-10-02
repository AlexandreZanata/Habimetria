package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalEvidence

// Hash the bytes we actually decode/draw, not a live DB row or a cache.
internal class ReportSources(
    private val originals: OriginalStore,
    private val fs: FileSystem,
    private val scaler: ImageScaler = platformScaler(),
) {
    fun original(evidence: LocalEvidence): ByteArray {
        val path = originals.originalPath(evidence.id).toPath()
        if (!fs.exists(path)) throw IncompleteReportException(listOf(evidence.id))
        require(fs.metadata(path).size == evidence.size && evidence.size <= MAX_ORIGINAL_BYTES) {
            "original size mismatch: ${evidence.id}"
        }
        val bytes = fs.read(path) { readByteArray() }
        require(originals.sha256Of(bytes) == evidence.sha256) { "original digest mismatch: ${evidence.id}" }
        return bytes
    }

    fun dimensions(evidence: LocalEvidence): ImageDims = imageDimensions(original(evidence))
        ?: throw LocalValidationException("undecodable original: ${evidence.id}")

    fun photo(evidence: LocalEvidence): ByteArray = scaler.scaleJpeg(original(evidence), PREVIEW_MAX_EDGE)
}
