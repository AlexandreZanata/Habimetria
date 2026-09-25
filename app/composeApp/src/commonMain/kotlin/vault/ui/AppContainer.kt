package vault.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import vault.api.VaultApi
import vault.api.platformEngine
import vault.queue.CaptureStore
import vault.queue.UploadPump

// AppContainer wires shared backend access for the UI.
// Created once per process; baseUrl points at staging/local API.
class AppContainer(
    val api: VaultApi,
    val store: CaptureStore,
    val pump: UploadPump,
    val filesDir: String,
    val photoCapture: PhotoCapture,
    val onQueued: () -> Unit = {},
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
)

fun createContainer(
    baseUrl: String,
    filesDir: String,
    photoCapture: PhotoCapture,
    onQueued: () -> Unit = {},
    fs: FileSystem = FileSystem.SYSTEM,
): AppContainer {
    val api = VaultApi(baseUrl, platformEngine())
    val store = CaptureStore(fs, "$filesDir/queue")
    val pump = UploadPump(store, api, fs)
    return AppContainer(api, store, pump, filesDir, photoCapture, onQueued)
}
