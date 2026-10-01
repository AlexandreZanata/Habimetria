package vault.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import vault.api.AppContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// AndroidPhotoCapture uses the shared ImageCapture (no preview needed for
// capture) with latency-first settings per docs/05-mobile/capture-ux.md.
// The camera provider is awaited asynchronously: blocking .get() on the
// main thread freezes input (ANR) and eats taps.
//
// Reliability (L4): permission is checked before touching the camera,
// target rotation follows the current display so landscape shots keep
// their orientation, failures carry explicit reasons, and the exact
// captured bytes are returned without re-encoding (the temp file is
// removed after reading). The use case is bound only when no screen
// holds it: with the preview open, CameraPreview already bound the
// shared instance and capture reuses it without rebinding.
class AndroidPhotoCapture : PhotoCapture {
    private val executor: Executor by lazy {
        ContextCompat.getMainExecutor(AppContext.app)
    }
    private val imageCapture: ImageCapture
        get() = SharedCamera.imageCapture

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(AppContext.app, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun provider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(AppContext.app)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            },
            executor,
        )
    }

    private suspend fun ensureBound() {
        val cameraProvider = provider()
        if (cameraProvider.isBound(imageCapture)) return
        try {
            cameraProvider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                SharedCamera.selector,
                imageCapture,
            )
        } catch (_: IllegalArgumentException) {
            // Raced with a screen bind holding the same instance.
        }
    }

    override suspend fun captureDetailed(): CaptureResult {
        if (!hasPermission()) return CaptureResult.Failure(CaptureFailure.PERMISSION_DENIED)
        try {
            ensureBound()
        } catch (_: Exception) {
            return CaptureResult.Failure(CaptureFailure.NOT_READY)
        }
        imageCapture.targetRotation = ContextCompat.getDisplayOrDefault(AppContext.app).rotation
        return suspendCancellableCoroutine { cont ->
            val out = File(AppContext.app.cacheDir, "cap-${System.nanoTime()}.jpg")
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(out).build(),
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val bytes = out.readBytes()
                        out.delete()
                        cont.resume(CaptureResult.Photo(bytes))
                    }

                    override fun onError(exc: ImageCaptureException) {
                        out.delete()
                        if (cont.isActive) cont.resume(CaptureResult.Failure(CaptureFailure.CAPTURE_FAILED))
                    }
                },
            )
        }
    }
}
