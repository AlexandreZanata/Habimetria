package vault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vault.api.AppContext
import vault.api.appFilesDir
import vault.local.LocalSession
import vault.local.NavigationProgress
import vault.local.SyncMode
import vault.local.openLocalSession
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.pdf.AndroidPlatformPdf
import vault.ui.AndroidPhotoCapture
import vault.ui.App
import vault.ui.ServerConfig
import vault.ui.UploadWorker
import vault.ui.createContainer

// Primary flow is local-first (L3): rooms, capture, review and seal run
// on-device with no account, payment or network. The API integration
// stays compiled and tested as an optional path (ServerConfig origin).
class MainActivity : ComponentActivity() {
    private val baseUrl = ServerConfig.DEFAULT_BASE_URL

    // Camera is blocking; location is best-effort (denied still captures).
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* denial surfaces per-photo as explicit status, never a block */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContext.app = applicationContext
        val missing = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missing.isNotEmpty()) permissions.launch(missing)
        val filesDir = appFilesDir()
        val session = openLocalSession(filesDir, SyncMode.LOCAL_ONLY)
        lifecycleScope.launch {
            session.startup()
        }
        val container = createContainer(
            baseUrl = baseUrl,
            filesDir = filesDir,
            photoCapture = AndroidPhotoCapture(),
            platformPdf = AndroidPlatformPdf(),
            onQueued = { UploadWorker.scheduleIfAllowed(applicationContext, baseUrl, SyncMode.LOCAL_ONLY) },
            syncMode = SyncMode.LOCAL_ONLY,
            local = session,
        )
        setContent { App(container, resumeNavigator(session)) }
    }

    // Resume where the user stopped; falls back to Home on fresh installs
    // or when the saved inspection no longer resolves (checked lazily).
    private fun resumeNavigator(session: LocalSession): VaultNavigator {
        val progress: NavigationProgress = session.navigation.load()
        val inspectionId = progress.inspectionId
        if (inspectionId.isNullOrBlank()) return VaultNavigator()
        val roomId = progress.roomId
        val label = progress.roomLabel
        return if (!roomId.isNullOrBlank() && !label.isNullOrBlank()) {
            VaultNavigator(Route.Room(inspectionId, roomId, label))
        } else {
            VaultNavigator(Route.Rooms(inspectionId))
        }
    }
}
