package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.queue.QueueItem
import vault.queue.QueueStatus

// Room: preview + shutter + queue truth + coverage.
// Shutter never blocks on network: capture → file → queue → worker.
@Composable
fun RoomScreen(
    container: AppContainer,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf(container.store.load()) }
    var coverage by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        queue = container.store.load()
        scope.launch {
            try {
                coverage = container.api.coverage(inspectionId).totalPercent
            } catch (_: Exception) { /* offline: keep last */ }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    val confirmed = queue.count { it.status == QueueStatus.CONFIRMED }
    val sending = queue.count { it.status == QueueStatus.UPLOADING || it.status == QueueStatus.QUEUED }
    val failed = queue.count { it.status == QueueStatus.FAILED }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CameraPreview(modifier = Modifier.fillMaxWidth().height(280.dp))
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val shot = container.photoCapture.capture()
                        if (shot == null) {
                            error = "Camera unavailable"
                        } else {
                            val name = "cap-${queue.size + 1}.jpg"
                            val path = "${container.filesDir}/photos/$name"
                            FileSystem.SYSTEM.createDirectories("${container.filesDir}/photos".toPath())
                            FileSystem.SYSTEM.write(path.toPath()) { write(shot.bytes) }
                            container.store.enqueue(
                                QueueItem(
                                    localId = name,
                                    inspectionId = inspectionId,
                                    roomId = null,
                                    roomKey = "kitchen",
                                    filePath = path,
                                    mime = shot.mime,
                                ),
                            )
                            container.onQueued()
                        }
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                    } finally {
                        busy = false
                        refresh()
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Saving…" else "Capture")
        }
        error?.let { Text("Error: $it") }
        Text("✓ $confirmed confirmed · ↑ $sending sending · ! $failed waiting")
        LinearProgressIndicator(progress = { coverage / 100f }, modifier = Modifier.fillMaxWidth())
        Text("Coverage: $coverage%")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
