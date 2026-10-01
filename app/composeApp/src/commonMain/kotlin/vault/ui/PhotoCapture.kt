package vault.ui

// PhotoCapture abstracts the shutter. Results are explicit: callers must
// distinguish denial, unreadiness and capture failure instead of showing
// a generic "camera unavailable" for every case.
enum class CaptureFailure {
    PERMISSION_DENIED, NOT_READY, CAPTURE_FAILED, UNAVAILABLE
}

sealed interface CaptureResult {
    data class Photo(val bytes: ByteArray, val mime: String = "image/jpeg") : CaptureResult
    data class Failure(val reason: CaptureFailure) : CaptureResult
}

fun captureErrorString(reason: CaptureFailure): String = when (reason) {
    CaptureFailure.PERMISSION_DENIED -> Strings.cameraPermissionDenied
    CaptureFailure.NOT_READY -> Strings.cameraBusy
    CaptureFailure.CAPTURE_FAILED -> Strings.cameraFailed
    CaptureFailure.UNAVAILABLE -> Strings.cameraUnavailable
}

interface PhotoCapture {
    suspend fun captureDetailed(): CaptureResult
}

class UnavailableCapture : PhotoCapture {
    override suspend fun captureDetailed(): CaptureResult =
        CaptureResult.Failure(CaptureFailure.UNAVAILABLE)
}
