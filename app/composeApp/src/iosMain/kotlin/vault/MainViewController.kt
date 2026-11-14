package vault

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import vault.local.openLocalSession
import vault.local.SyncMode
import vault.api.appFilesDir
import vault.pdf.IosPlanRenderer
import vault.pdf.PlatformPdf
import vault.ui.App
import vault.ui.IosPhotoCapture
import vault.ui.createContainer
import vault.account.AccountApi
import vault.account.AccountSessions
import vault.account.IosSessionVault
import vault.api.platformEngine
import platform.Foundation.NSUUID

// Entry point embedded by iosApp's ContentView. The Swift host injects
// its PDFKit/CoreImage pipeline; capture runs through AVFoundation (L4).
fun MainViewController(platformPdf: PlatformPdf) = ComposeUIViewController {
    val session = remember { openLocalSession(appFilesDir(), SyncMode.LOCAL_ONLY) }
    LaunchedEffect(session) { session.startup() }
    val container = remember { createContainer(
            baseUrl = "http://localhost:8080",
            filesDir = appFilesDir(),
            photoCapture = IosPhotoCapture(),
            platformPdf = platformPdf,
            planRenderer = IosPlanRenderer(),
            local = session,
            syncMode = SyncMode.LOCAL_ONLY,
            filePicker = vault.ui.IosFilePicker(),
            accountSessions = AccountSessions(AccountApi("http://localhost:8080", platformEngine()),
                IosSessionVault(appFilesDir()), { NSUUID().UUIDString.lowercase() }, "ios", "iPhone"),
            accountFactory = { origin -> AccountSessions(AccountApi(origin, platformEngine()),
                IosSessionVault(appFilesDir()), { NSUUID().UUIDString.lowercase() }, "ios", "iPhone") },
        ) }
    App(container)
}
