package vault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
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
import vault.pdf.AndroidPlanRenderer
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
    private val photoCapture = AndroidPhotoCapture()

    // Capture delegates to the system camera (no in-app preview), so no
    // CAMERA permission is needed here; location is best-effort per photo
    // (denied still captures).
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> photoCapture.onTakePictureResult(success) }

    // Location is best-effort (denied still captures).
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* denial surfaces per-photo as explicit status, never a block */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContext.app = applicationContext
        photoCapture.attach(takePicture)
        val missing = arrayOf(
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
            photoCapture = photoCapture,
            platformPdf = AndroidPlatformPdf(),
            onQueued = { UploadWorker.scheduleIfAllowed(applicationContext, baseUrl, SyncMode.LOCAL_ONLY) },
            syncMode = SyncMode.LOCAL_ONLY,
            local = session,
            planRenderer = AndroidPlanRenderer(),
        )
        val navigator = resumeNavigator(session)
        wireSystemBack(navigator)
        setContent { App(container, navigator) }
    }

    // System back mirrors the in-app Voltar (same floors): it never
    // kills the app mid-journey, only Home exits.
    private fun wireSystemBack(navigator: VaultNavigator) {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val exit = when (val r = navigator.route.value) {
                        Route.Home -> true
                        is Route.Rooms -> {
                            navigator.goBackOr(Route.Home)
                            false
                        }
                        is Route.Room -> {
                            navigator.goBackOr(Route.Rooms(r.inspectionId))
                            false
                        }
                        else -> !navigator.goBack()
                    }
                    if (exit) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
    }

    // Resume where the user stopped with a full trail, never a bare
    // single route: system and in-app back always walk
    // Room → Rooms → Home instead of dying or ping-ponging.
    private fun resumeNavigator(session: LocalSession): VaultNavigator {
        val navigator = VaultNavigator()
        val progress: NavigationProgress = session.navigation.load()
        val inspectionId = progress.inspectionId
        if (inspectionId.isNullOrBlank()) return navigator
        navigator.navigate(Route.Rooms(inspectionId))
        val roomId = progress.roomId
        val label = progress.roomLabel
        if (!roomId.isNullOrBlank() && !label.isNullOrBlank()) {
            navigator.navigate(Route.Room(inspectionId, roomId, label))
        }
        return navigator
    }
}
