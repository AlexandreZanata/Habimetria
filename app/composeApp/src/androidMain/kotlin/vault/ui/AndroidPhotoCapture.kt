package vault.ui

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import vault.api.AppContext

// System-camera capture (ACTION_IMAGE_CAPTURE via TakePicture). No
// embedded preview, no CameraX lifecycle to leak: the user taps
// Fotografar, the camera opens full-screen, and the exact JPEG bytes
// come back through the FileProvider cache file (removed after reading).
// Location stays best-effort at shutter time; denial never blocks.
//
// Reliability (L4): the launcher is registered by MainActivity (an
// Activity contract cannot start from an application context). A missing
// camera app or a cancelled shot maps to explicit failures; the pending
// continuation resumes exactly once.
class AndroidPhotoCapture : PhotoCapture {
    private var launcher: ActivityResultLauncher<Uri>? = null
    private var pending: Continuation<Boolean>? = null
    private var pendingFile: File? = null

    fun attach(launcher: ActivityResultLauncher<Uri>) {
        this.launcher = launcher
    }

    fun onTakePictureResult(success: Boolean) {
        pending?.let { cont ->
            pending = null
            cont.resume(success)
        }
    }

    override suspend fun captureDetailed(): CaptureResult {
        val takePicture = launcher ?: return CaptureResult.Failure(CaptureFailure.UNAVAILABLE)
        val out = File(AppContext.app.cacheDir, "cap-${System.nanoTime()}.jpg")
        val uri = FileProvider.getUriForFile(AppContext.app, "vault.app.fileprovider", out)
        pendingFile = out
        val success = suspendCancellableCoroutine { cont ->
            pending?.let { old -> old.resume(false) }
            pending = cont
            takePicture.launch(uri)
        }
        pendingFile = null
        if (!success || !out.exists()) {
            out.delete()
            return CaptureResult.Failure(CaptureFailure.CAPTURE_FAILED)
        }
        val bytes = out.readBytes()
        out.delete()
        if (bytes.isEmpty()) return CaptureResult.Failure(CaptureFailure.CAPTURE_FAILED)
        return CaptureResult.Photo(bytes)
    }
}
