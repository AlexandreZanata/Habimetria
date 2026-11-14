package vault

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import vault.nav.RestoreFacts
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.nav.trailFor
import vault.nav.validatedTrail
import vault.pdf.AndroidPlanRenderer
import vault.pdf.AndroidPlatformPdf
import vault.ui.AndroidPhotoCapture
import vault.ui.App
import vault.ui.ServerConfig
import vault.ui.UploadWorker
import vault.ui.createContainer
import vault.account.AccountApi
import vault.account.AccountSessions
import vault.account.AndroidSessionVault
import vault.api.platformEngine
import java.util.UUID

// Primary flow is local-first (L3): rooms, capture, review and seal run
// on-device with no account, payment or network. The API integration
// stays compiled and tested as an optional path (ServerConfig origin).
class MainActivity : ComponentActivity() {
    private val baseUrl = ServerConfig.DEFAULT_BASE_URL
    private val photoCapture = AndroidPhotoCapture()
    private val filePicker = vault.ui.AndroidFilePicker()
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { filePicker.onResult(it) }

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
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        AppContext.app = applicationContext
        photoCapture.attach(takePicture)
        filePicker.attach(openDocument)
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
            filePicker = filePicker,
            accountSessions = AccountSessions(AccountApi(baseUrl, platformEngine()), AndroidSessionVault(applicationContext),
                { UUID.randomUUID().toString() }, "android", "Android"),
            accountFactory = { origin -> AccountSessions(AccountApi(origin, platformEngine()), AndroidSessionVault(applicationContext),
                { UUID.randomUUID().toString() }, "android", "Android") },
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
                        Route.Properties, Route.Inspections, Route.Settings -> {
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

    // Startup revalidation lives in shared vault.nav.validatedTrail so
    // JVM tests cover the stale-route fallbacks (deleted vs archived,
    // missing vs sealed); the activity only restores the returned trail.
}
