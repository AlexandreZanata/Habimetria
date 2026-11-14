package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var items by remember(inspectionId) { mutableStateOf<List<QueueItem>?>(null) }
    var error by remember(inspectionId) { mutableStateOf<String?>(null) }
    var refreshCount by remember(inspectionId) { mutableStateOf(0) }
    var flagged by remember { mutableStateOf(setOf<String>()) }
    var sheetFor by remember { mutableStateOf<QueueItem?>(null) }

    LaunchedEffect(inspectionId, refreshCount) {
        error = null
        try {
            items = withContext(Dispatchers.Default) {
                container.store.load().filter { it.inspectionId == inspectionId }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = userErrorMessage(e)
        }
    }

    val currentItems = items
    val showSkeleton = rememberLoadingVisible(currentItems == null && error == null)

    ScreenColumn {
        ScreenTitle(currentItems?.let { "${Strings.review} — ${it.size} ${Strings.photosSuffix}" } ?: Strings.review)
        ErrorText(error)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showSkeleton) {
                item(key = "loading") {
                    LoadingSkeleton(spacing = 8.dp) { repeat(3) { ReviewRowSkeleton() } }
                }
            }
            items(currentItems.orEmpty(), key = { it.localId }) { item ->
                ReviewRow(
                    item = item,
                    hasFinding = item.evidenceId in flagged,
                    onIssue = { sheetFor = item },
                )
            }
        }
        VaultButton(text = Strings.sealTitle, onClick = onSeal)
        VaultButton(text = Strings.back, onClick = onBack)
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
                    refreshCount++
                },
                onDismiss = { sheetFor = null },
            )
        }
    }
}

@Composable
private fun ReviewRowSkeleton() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonThumbnail(72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkeletonLine(0.66f)
            SkeletonLine(0.9f, 14.dp)
        }
        SkeletonAction(Modifier.width(92.dp), height = 48.dp)
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
            Image(
                bitmap = thumb,
                contentDescription = "${Strings.photosSuffix}: ${item.roomKey}",
                modifier = Modifier.size(72.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.roomKey)
            val state = when (item.status) {
                QueueStatus.CONFIRMED -> "✓ ${Strings.queueConfirmed} #${item.evidenceId?.take(8)}"
                QueueStatus.UPLOADING, QueueStatus.QUEUED -> "↑ ${Strings.queueSending}…"
                QueueStatus.FAILED -> "! ${Strings.queueWaiting}"
            }
            Text(state)
            if (hasFinding) Text(Strings.issueRecorded)
        }
        if (item.evidenceId != null && item.roomId != null && !hasFinding) {
            Button(onClick = onIssue) { Text(Strings.reportIssue) }
        }
    }
}
