package vault

import androidx.compose.ui.window.ComposeUIViewController
import vault.api.appFilesDir
import vault.ui.App
import vault.ui.UnavailableCapture
import vault.ui.createContainer

// Entry point embedded by iosApp's ContentView.
// Camera + background upload land in M3; UI compiles and runs today.
fun MainViewController() = ComposeUIViewController {
    App(
        createContainer(
            baseUrl = "http://localhost:8080",
            filesDir = appFilesDir(),
            photoCapture = UnavailableCapture(),
        ),
    )
}
