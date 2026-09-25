package vault.ui

// PhotoCapture abstracts the shutter. Android binds CameraX once;
// iOS throws until the Swift host wires AVCapture in M3.
interface PhotoCapture {
    suspend fun capture(): CapturedPhoto?
}

data class CapturedPhoto(
    val bytes: ByteArray,
    val mime: String = "image/jpeg",
)

class UnavailableCapture : PhotoCapture {
    override suspend fun capture(): CapturedPhoto? = null
}
