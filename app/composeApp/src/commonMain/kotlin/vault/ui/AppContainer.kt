package vault.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import vault.api.VaultApi
import vault.api.platformEngine
import vault.local.LocalSession
import vault.local.SyncMode
import vault.queue.CaptureStore
import vault.queue.UploadPump

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
)

fun createContainer(
    baseUrl: String,
    filesDir: String,
    photoCapture: PhotoCapture,
    platformPdf: PlatformPdf,
    onQueued: () -> Unit = {},
    fs: FileSystem = FileSystem.SYSTEM,
    syncMode: SyncMode = SyncMode.WITH_SERVER_SYNC,
    local: LocalSession? = null,
): AppContainer {
    val api = VaultApi(baseUrl, platformEngine())
    val store = CaptureStore(fs, "$filesDir/queue")
    val pump = UploadPump(store, api, fs)
    return AppContainer(api, store, pump, filesDir, photoCapture, platformPdf, onQueued, syncMode = syncMode, local = local)
}
