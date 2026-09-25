package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import okio.Path.Companion.toPath
import vault.queue.QueueItem
import vault.queue.QueueStatus

// Review: local photo grid. Tap a confirmed photo to report a finding.
// Pending photos show their queue state; nothing claims "saved" early.
@Composable
fun ReviewScreen(
    container: AppContainer,
    inspectionId: String,
    onSeal: () -> Unit,
    onBack: () -> Unit,
) {
    var queue by remember { mutableStateOf(container.store.load()) }
    var findingFor by remember { mutableStateOf<QueueItem?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review — ${queue.size} photos")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(queue, key = { it.localId }) { item ->
                ReviewThumb(item, onTap = {
                    if (item.status == QueueStatus.CONFIRMED && item.evidenceId != null) {
                        findingFor = item
                    }
                })
            }
        }
        findingFor?.let { item ->
            FindingSheet(
                container = container,
                roomId = item.roomId ?: return@let,
                evidenceId = item.evidenceId ?: return@let,
                onSaved = { findingFor = null },
                onCancel = { findingFor = null },
            )
        }
        Button(onClick = onSeal, modifier = Modifier.fillMaxWidth()) {
            Text("Seal inspection")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun ReviewThumb(item: QueueItem, onTap: () -> Unit) {
    Column {
        AsyncImage(
            model = item.filePath.toPath(),
            contentDescription = item.roomKey,
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )
        Button(onClick = onTap, enabled = item.status == QueueStatus.CONFIRMED) {
            Text(
                when (item.status) {
                    QueueStatus.CONFIRMED -> "+ finding"
                    QueueStatus.FAILED -> "! retry"
                    else -> "↑ waiting"
                },
            )
        }
    }
}
