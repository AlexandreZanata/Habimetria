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
import vault.local.STATUS_SEALED
import vault.local.SyncMode
import vault.local.openLocalSession
import vault.nav.RestoreFacts
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.nav.trailFor
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
        lifecycleScope.launch {
            session.startup()
            navigator.restoreStack(validatedTrail(session))
        }
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
                        Route.Properties, Route.Inspections, Route.Reports -> {
                            navigator.selectDestination(Route.Home)
                            false
                        }
                        is Route.PropertyDetail -> {
                            navigator.goBackOr(Route.Properties)
                            false
                        }
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
    // Room → Rooms → Home instead of dying or ping-ponging. The first
    // frame restores optimistically; startup re-resolves against the
    // repository so removed targets and newly sealed inspections correct
    // to list roots or read-only review without false trust.
    private fun resumeNavigator(session: LocalSession): VaultNavigator {
        val navigator = VaultNavigator()
        val progress: NavigationProgress = session.navigation.load()
        navigator.restoreStack(
            trailFor(
                progress,
                RestoreFacts(
                    inspectionExists = true,
                    inspectionSealed = false,
                    roomExists = true,
                    propertyExists = true,
                ),
            ),
        )
        return navigator
    }

    private suspend fun validatedTrail(session: LocalSession): List<Route> {
        val progress: NavigationProgress = session.navigation.load()
        val inspectionId = progress.inspectionId?.takeIf { it.isNotBlank() }
        val inspection = inspectionId?.let { session.repository.getInspection(it) }
        val roomId = progress.roomId?.takeIf { it.isNotBlank() }
        val roomExists = if (inspectionId == null || roomId == null) {
            false
        } else {
            session.repository.roomsFor(inspectionId).any { it.id == roomId }
        }
        val propertyId = progress.propertyId?.takeIf { it.isNotBlank() }
        val propertyExists = if (propertyId == null) {
            false
        } else {
            session.repository.listProperties().any { it.id == propertyId }
        }
        return trailFor(
            progress,
            RestoreFacts(
                inspectionExists = inspectionId == null || inspection != null,
                inspectionSealed = inspection?.status == STATUS_SEALED,
                roomExists = roomExists,
                propertyExists = propertyExists,
            ),
        )
    }
}
