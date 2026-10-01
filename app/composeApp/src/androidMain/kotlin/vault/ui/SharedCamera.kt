package vault.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

// Single Preview + ImageCapture shared by the preview screen and the
// shutter. CameraX allows one Preview per camera: binding a fresh Preview
// on every screen visit without unbinding leaks the old one, the next
// bind fails, and the view freezes on a dead surface. Every bind goes
// through here; leaving the screen always releases the camera.
object SharedCamera {
    val selector = CameraSelector.DEFAULT_BACK_CAMERA

    val preview: Preview by lazy { Preview.Builder().build() }

    val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(85)
            .build()
    }

    fun bind(provider: ProcessCameraProvider, owner: LifecycleOwner) {
        provider.unbindAll()
        provider.bindToLifecycle(owner, selector, preview, imageCapture)
    }

    fun release(provider: ProcessCameraProvider) {
        provider.unbindAll()
    }
}
