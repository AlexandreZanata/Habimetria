package vault.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import vault.api.VaultApi
import vault.api.platformEngine
import vault.account.AccountSessions
import vault.local.HomeBannerPreference
import vault.local.LocalSession
import vault.local.PlanRenderer
import vault.local.SyncMode
import vault.queue.CaptureStore
import vault.queue.UploadPump
import vault.sync.DbMirrorStore
import vault.sync.MirrorStore
import vault.sync.ServerMirror

import vault.pdf.PlatformPdf

// AppContainer wires shared backend access for the UI.
// Created once per process; baseUrl points at staging/local API.
// syncMode selects the production path: LOCAL_ONLY disables network
// workers (L1 gate); WITH_SERVER_SYNC keeps the optional API integration.
// Per-screen injection of LocalSession completes with the L2/L3 rewiring.
class AppContainer(
    val api: VaultApi,
    val store: CaptureStore,
    val pump: UploadPump,
    val filesDir: String,
    val photoCapture: PhotoCapture,
    val platformPdf: PlatformPdf,
    val onQueued: () -> Unit = {},
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val syncMode: SyncMode = SyncMode.WITH_SERVER_SYNC,
    val local: LocalSession? = null,
    val planRenderer: PlanRenderer? = null,
    val filePicker: LocalFilePicker? = null,
    val homeBannerPreference: HomeBannerPreference = HomeBannerPreference(FileSystem.SYSTEM, filesDir),
    val account: AccountSessions? = null,
) {
    // serverMirror is the only path from UI layers to VaultApi: screens
    // ask the container for a mirror instead of calling the API
    // directly. Null when the local core is absent; LOCAL_ONLY mirrors
    // report SkippedLocalOnly without network traffic.
    fun serverMirror(store: MirrorStore? = null): ServerMirror? {
        val session = local ?: return null
        return ServerMirror(session.db, api, session.syncMode, store ?: DbMirrorStore(session.db))
    }
}

fun createContainer(
    baseUrl: String,
    filesDir: String,
    photoCapture: PhotoCapture,
    platformPdf: PlatformPdf,
    onQueued: () -> Unit = {},
    fs: FileSystem = FileSystem.SYSTEM,
    syncMode: SyncMode = SyncMode.WITH_SERVER_SYNC,
    local: LocalSession? = null,
    planRenderer: PlanRenderer? = null,
    filePicker: LocalFilePicker? = null,
    accountSessions: AccountSessions? = null,
): AppContainer {
    val api = VaultApi(baseUrl, platformEngine())
    val store = CaptureStore(fs, "$filesDir/queue")
    val pump = UploadPump(store, api, fs)
    return AppContainer(
        api, store, pump, filesDir, photoCapture, platformPdf, onQueued,
        syncMode = syncMode, local = local, planRenderer = planRenderer, filePicker = filePicker,
        homeBannerPreference = HomeBannerPreference(fs, filesDir),
        account = accountSessions,
    )
}
