package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.api.Coverage
import vault.nav.useSideBySide
import vault.nav.windowClassFor
import vault.queue.QueueItem
import vault.queue.QueueStatus

// Room: preview + shutter + queue truth + coverage.
// Shutter never blocks on network: capture → file → queue → worker.
// Compact phones stack preview above controls; wide windows
// (landscape, tablet, foldable) place them side by side.
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
    var coverage by remember { mutableStateOf<Coverage?>(null) }
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
    val capture: () -> Unit = {
        busy = true
        error = null
        scope.launch {
            try {
                val shot = container.photoCapture.capture()
                if (shot == null) {
                    error = Strings.cameraUnavailable
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
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (useSideBySide(windowClassFor(maxWidth.value.toInt()))) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CameraPreview(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { contentDescription = Strings.cameraPreview },
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RoomControls(
                        roomId, mine, coverage,
                        busy, error, capture, onOpenRoom, onReview, onBack,
                    )
                }
            }
        } else {
            ScreenColumn {
                ScreenTitle(roomLabel)
                CameraPreview(
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                        .semantics { contentDescription = Strings.cameraPreview },
                )
                RoomControls(
                    roomId, mine, coverage,
                    busy, error, capture, onOpenRoom, onReview, onBack,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.RoomControls(
    roomId: String,
    mine: List<QueueItem>,
    coverage: Coverage?,
    busy: Boolean,
    error: String?,
    onCapture: () -> Unit,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    val confirmed = mine.count { it.status == QueueStatus.CONFIRMED }
    val sending = mine.count { it.status == QueueStatus.UPLOADING || it.status == QueueStatus.QUEUED }
    val failed = mine.count { it.status == QueueStatus.FAILED }
    VaultButton(
        text = if (busy) Strings.saving else Strings.capture,
        onClick = onCapture,
        enabled = !busy,
    )
    ErrorText(error)
    Text("✓ $confirmed ${Strings.queueConfirmed} · ↑ $sending ${Strings.queueSending} · ! $failed ${Strings.queueWaiting}")
    val total = coverage?.totalPercent ?: 0
    LinearProgressIndicator(progress = { total / 100f }, modifier = Modifier.fillMaxWidth())
    Text("${Strings.coverage}: $total%")
    coverage?.rooms?.forEach { room ->
        Text("${room.label}: ${room.percent}% (${room.photos} ${Strings.photosSuffix})")
    }
    // Primary action: next room in template order, Review on the last.
    // Coverage rooms come ordered by sort from the backend.
    val order = coverage?.rooms ?: emptyList()
    val nextIdx = order.indexOfFirst { it.roomId == roomId } + 1
    if (nextIdx in order.indices) {
        val next = order[nextIdx]
        VaultButton(
            text = "${Strings.next}: ${next.label} →",
            onClick = { onOpenRoom(next.roomId, next.label) },
        )
    } else if (order.isNotEmpty()) {
        VaultButton(text = "${Strings.review} →", onClick = onReview)
    }
    VaultButton(text = Strings.back, onClick = onBack)
}
