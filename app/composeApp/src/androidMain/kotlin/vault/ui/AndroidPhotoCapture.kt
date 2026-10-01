package vault.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
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

// AndroidPhotoCapture binds ImageCapture once (no preview needed for
// capture) with latency-first settings per docs/05-mobile/capture-ux.md.
// The camera provider is awaited asynchronously: blocking .get() on the
// main thread freezes input (ANR) and eats taps.
//
// Reliability (L4): permission is checked before touching the camera,
// target rotation follows the current display so landscape shots keep
// their orientation, failures carry explicit reasons, and the exact
// captured bytes are returned without re-encoding (the temp file is
// removed after reading).
class AndroidPhotoCapture : PhotoCapture {
    private val executor: Executor by lazy {
        ContextCompat.getMainExecutor(AppContext.app)
    }
    private val selector = CameraSelector.DEFAULT_BACK_CAMERA
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(85)
        .build()
    private var bound = false

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
        if (bound) return
        val cameraProvider = provider()
        try {
            cameraProvider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                selector,
                imageCapture,
            )
            bound = true
        } catch (_: IllegalArgumentException) {
            bound = true // already bound on this lifecycle
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
