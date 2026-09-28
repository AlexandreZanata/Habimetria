package vault.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Preview-only binding, awaited off the main thread. Capture uses
// AndroidPhotoCapture's own ImageCapture; CameraX merges use cases.
@Composable
actual fun CameraPreview(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(previewView) {
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider
        try {
            provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        } catch (_: IllegalArgumentException) {
            // lifecycle gone; preview stays black, capture still works
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
