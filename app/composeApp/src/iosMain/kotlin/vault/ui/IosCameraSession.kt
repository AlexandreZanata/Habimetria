package vault.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation

// Shared AVCapture pipeline: one session feeds the preview layer and the
// photo output. Configured once, kept alive across captures so the
// shutter stays fast. Compiled on Linux (klib); device pass lands in L8.
@OptIn(ExperimentalForeignApi::class)
internal object IosCameraSession {
    val session = AVCaptureSession()
    val photoOutput = AVCapturePhotoOutput()
    private var configured = false

    fun authorizationStatus(): AVAuthorizationStatus =
        AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)

    fun isAuthorized(): Boolean =
        authorizationStatus() == AVAuthorizationStatusAuthorized

    fun needsRequest(): Boolean =
        authorizationStatus() == AVAuthorizationStatusNotDetermined

    // Returns false when no back camera exists or input/output fails.
    fun ensureConfigured(): Boolean {
        if (configured) return true
        val device = discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack,
        ).devices.firstOrNull() as? AVCaptureDevice ?: return false
        val input = deviceInputWithDevice(device = device, error = null) ?: return false
        session.beginConfiguration()
        try {
            if (!session.canAddInput(input)) return false
            session.addInput(input)
            if (!session.canAddOutput(photoOutput)) return false
            session.addOutput(photoOutput)
            session.sessionPreset = AVCaptureSessionPresetPhoto
        } finally {
            session.commitConfiguration()
        }
        configured = true
        return true
    }

    fun startIfNeeded() {
        if (!session.isRunning()) session.startRunning()
    }

    // Back-camera mapping: device landscape-left means the sensor sees
    // landscape-right. Unknown orientations keep the previous value.
    fun applyOrientation() {
        val connection: AVCaptureConnection =
            photoOutput.connectionWithMediaType(AVMediaTypeVideo) ?: return
        if (!connection.isVideoOrientationSupported()) return
        val orientation: AVCaptureVideoOrientation = when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationPortrait
        }
        connection.videoOrientation = orientation
    }
}

internal fun requestCameraAccess(completion: (Boolean) -> Unit) {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo, completion)
}
