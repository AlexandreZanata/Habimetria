package vault.ui

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

// AndroidPhotoCapture binds ImageCapture once (no preview needed for
// capture) with latency-first settings per docs/05-mobile/capture-ux.md.
class AndroidPhotoCapture : PhotoCapture {
    private val executor: Executor by lazy {
        ContextCompat.getMainExecutor(AppContext.app)
    }
    private val capture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(85)
            .build()
            .also { bind(it) }
    }

    private fun bind(imageCapture: ImageCapture) {
        val provider = ProcessCameraProvider.getInstance(AppContext.app).get()
        provider.unbindAll()
        provider.bindToLifecycle(
            ProcessLifecycleOwner.get(),
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture,
        )
    }

    override suspend fun capture(): CapturedPhoto? = suspendCancellableCoroutine { cont ->
        val out = File(AppContext.app.cacheDir, "cap-${System.nanoTime()}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(out).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cont.resume(CapturedPhoto(out.readBytes()))
                }

                override fun onError(exc: ImageCaptureException) {
                    if (cont.isActive) cont.resume(null)
                }
            },
        )
    }
}
