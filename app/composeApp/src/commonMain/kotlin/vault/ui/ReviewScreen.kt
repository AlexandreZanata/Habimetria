package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.queue.QueueItem
import vault.queue.QueueStatus

// Review: every queued photo with server truth + per-photo findings.
// Findings need a confirmed evidence_id; unconfirmed rows explain why.
@Composable
fun ReviewScreen(
    container: AppContainer,
    inspectionId: String,
    onSeal: () -> Unit,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf(container.store.load().filter { it.inspectionId == inspectionId }) }
    var flagged by remember { mutableStateOf(setOf<String>()) }
    var sheetFor by remember { mutableStateOf<QueueItem?>(null) }

    fun refresh() {
        items = container.store.load().filter { it.inspectionId == inspectionId }
    }

    LaunchedEffect(inspectionId) { refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review — ${items.size} photos")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.localId }) { item ->
                ReviewRow(
                    item = item,
                    hasFinding = item.evidenceId in flagged,
                    onIssue = { sheetFor = item },
                )
            }
        }
        Button(onClick = onSeal, modifier = Modifier.fillMaxWidth()) {
            Text("Seal inspection")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
    sheetFor?.let { item ->
        val evidenceId = item.evidenceId
        val roomId = item.roomId
        if (evidenceId != null && roomId != null) {
            FindingSheet(
                container = container,
                roomId = roomId,
                evidenceId = evidenceId,
                onDone = {
                    flagged = flagged + evidenceId
                    sheetFor = null
                    refresh()
                },
                onDismiss = { sheetFor = null },
            )
        }
    }
}

@Composable
private fun ReviewRow(
    item: QueueItem,
    hasFinding: Boolean,
    onIssue: () -> Unit,
) {
    val thumb = remember(item.filePath) { decodeThumbnail(item.filePath) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (thumb != null) {
            Image(bitmap = thumb, contentDescription = null, modifier = Modifier.size(72.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.roomKey)
            val state = when (item.status) {
                QueueStatus.CONFIRMED -> "✓ confirmed #${item.evidenceId?.take(8)}"
                QueueStatus.UPLOADING, QueueStatus.QUEUED -> "↑ sending…"
                QueueStatus.FAILED -> "! tap capture to retry"
            }
            Text(state)
            if (hasFinding) Text("Issue recorded")
        }
        if (item.evidenceId != null && item.roomId != null && !hasFinding) {
            Button(onClick = onIssue) { Text("Issue") }
        }
    }
}
