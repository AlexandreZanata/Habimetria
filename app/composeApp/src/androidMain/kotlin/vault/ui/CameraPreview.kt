package vault.ui

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Preview + capture bound together to the screen lifecycle and released
// on dispose. Revisiting the screen rebinds cleanly instead of leaking a
// Preview that freezes the next one. The provider fetch stays off the
// main thread; a dispose racing the fetch skips the stale bind.
@Composable
actual fun CameraPreview(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycle, previewView) {
        var discarded = false
        scope.launch(Dispatchers.IO) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            withContext(Dispatchers.Main) {
                if (!discarded) {
                    SharedCamera.preview.surfaceProvider = previewView.surfaceProvider
                    SharedCamera.bind(cameraProvider, lifecycle)
                    provider = cameraProvider
                }
            }
        }
        onDispose {
            discarded = true
            provider?.let { SharedCamera.release(it) }
            provider = null
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
