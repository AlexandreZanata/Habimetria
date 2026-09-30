package vault

import androidx.compose.ui.window.ComposeUIViewController
import vault.api.appFilesDir
import vault.pdf.IosPlanRenderer
import vault.pdf.PlatformPdf
import vault.ui.App
import vault.ui.IosPhotoCapture
import vault.ui.createContainer

// Entry point embedded by iosApp's ContentView. The Swift host injects
// its PDFKit/CoreImage pipeline; capture runs through AVFoundation (L4).
fun MainViewController(platformPdf: PlatformPdf) = ComposeUIViewController {
    App(
        createContainer(
            baseUrl = "http://localhost:8080",
            filesDir = appFilesDir(),
            photoCapture = IosPhotoCapture(),
            platformPdf = platformPdf,
            planRenderer = IosPlanRenderer(),
        ),
    )
}
