package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView

// Live AVFoundation preview sharing the capture session. Permission is
// requested once; denial shows an explicit message instead of a black box.
@Composable
actual fun CameraPreview(modifier: Modifier) {
    var authorized by remember { mutableStateOf(IosCameraSession.isAuthorized()) }
    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!authorized && IosCameraSession.needsRequest() && !asked) {
            asked = true
            requestCameraAccess { granted -> authorized = granted }
        }
    }
    if (authorized) {
        UIKitView(
            factory = { PreviewView() },
            modifier = modifier,
        )
    } else {
        Box(modifier) {
            Text(if (asked) Strings.cameraPermissionDenied else Strings.cameraComingSoon)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PreviewView : UIView(CGRectZero.readValue()) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = IosCameraSession.session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.setFrame(bounds)
    }
}
