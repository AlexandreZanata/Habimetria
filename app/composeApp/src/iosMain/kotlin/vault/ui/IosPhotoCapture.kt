package vault.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.fileDataRepresentation
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

// AVFoundation capture: exact JPEG bytes from the photo output, no
// re-encoding. Never blocks waiting for hardware; every failure maps to
// an explicit CaptureFailure the UI can explain.
class IosPhotoCapture : PhotoCapture {

    override suspend fun captureDetailed(): CaptureResult {
        if (!IosCameraSession.isAuthorized()) {
            if (IosCameraSession.needsRequest()) {
                val granted = requestAccess()
                if (!granted) return CaptureResult.Failure(CaptureFailure.PERMISSION_DENIED)
            } else {
                return CaptureResult.Failure(CaptureFailure.PERMISSION_DENIED)
            }
        }
        val ready = withContext(Dispatchers.IO) {
            IosCameraSession.ensureConfigured()
        }
        if (!ready) return CaptureResult.Failure(CaptureFailure.NOT_READY)
        return withContext(Dispatchers.IO) {
            IosCameraSession.startIfNeeded()
            IosCameraSession.applyOrientation()
            takePhoto()
        }
    }

    private suspend fun requestAccess(): Boolean = suspendCancellableCoroutine { cont ->
        requestCameraAccess { granted ->
            if (cont.isActive) cont.resume(granted)
        }
    }

    private suspend fun takePhoto(): CaptureResult = suspendCancellableCoroutine { cont ->
        val settings = AVCapturePhotoSettings.photoSettings()
        val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?,
            ) {
                if (error != null || !cont.isActive) {
                    if (cont.isActive) cont.resume(CaptureResult.Failure(CaptureFailure.CAPTURE_FAILED))
                    return
                }
                val bytes = didFinishProcessingPhoto.fileDataRepresentation()?.toByteArray()
                if (bytes == null || bytes.isEmpty()) {
                    cont.resume(CaptureResult.Failure(CaptureFailure.CAPTURE_FAILED))
                } else {
                    cont.resume(CaptureResult.Photo(bytes))
                }
            }
        }
        IosCameraSession.photoOutput.capturePhotoWithSettings(settings, delegate)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val out = ByteArray(size)
    memcpy(out.refTo(0), bytes, length)
    return out
}
