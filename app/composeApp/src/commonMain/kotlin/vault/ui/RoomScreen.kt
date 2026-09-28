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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    roomId: String,
    roomLabel: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf(container.store.load()) }
    var coverage by remember { mutableStateOf<vault.api.Coverage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            queue = withContext(Dispatchers.Default) { container.store.load() }
            try {
                coverage = container.api.coverage(inspectionId)
            } catch (_: Exception) { /* offline: keep last */ }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    val mine = queue.filter { it.inspectionId == inspectionId }
    val confirmed = mine.count { it.status == QueueStatus.CONFIRMED }
    val sending = mine.count { it.status == QueueStatus.UPLOADING || it.status == QueueStatus.QUEUED }
    val failed = mine.count { it.status == QueueStatus.FAILED }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(roomLabel)
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
                            val name = "cap-${mine.size + 1}-${roomId.take(4)}.jpg"
                            val path = "${container.filesDir}/photos/$name"
                            withContext(Dispatchers.Default) {
                                FileSystem.SYSTEM.createDirectories("${container.filesDir}/photos".toPath())
                                FileSystem.SYSTEM.write(path.toPath()) { write(shot.bytes) }
                            }
                            withContext(Dispatchers.Default) {
                                container.store.enqueue(
                                    QueueItem(
                                        localId = name,
                                        inspectionId = inspectionId,
                                        roomId = roomId,
                                        roomKey = roomLabel,
                                        filePath = path,
                                        mime = shot.mime,
                                    ),
                                )
                            }
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
        val total = coverage?.totalPercent ?: 0
        LinearProgressIndicator(progress = { total / 100f }, modifier = Modifier.fillMaxWidth())
        Text("Coverage: $total%")
        coverage?.rooms?.forEach { room ->
            Text("${room.label}: ${room.percent}% (${room.photos} photos)")
        }
        // Primary action: next room in template order, Review on the last.
        // Coverage rooms come ordered by sort from the backend.
        val order = coverage?.rooms ?: emptyList()
        val nextIdx = order.indexOfFirst { it.roomId == roomId } + 1
        if (nextIdx in order.indices) {
            val next = order[nextIdx]
            Button(
                onClick = { onOpenRoom(next.roomId, next.label) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next: ${next.label} →")
            }
        } else if (order.isNotEmpty()) {
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                Text("Review and seal →")
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
