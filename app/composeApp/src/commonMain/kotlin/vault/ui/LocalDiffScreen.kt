package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import vault.local.DiffSignal
import vault.local.LocalSession
import vault.local.db.VisualDiff
import vault.local.parseDiffSignals

// V1.5 visual-difference list: entry-vs-exit positions where an offline
// heuristic fired. Review or dismiss each row; nothing here assigns
// liability and nothing retags the comparison by itself.
@Composable
fun LocalDiffScreen(
    session: LocalSession,
    exitInspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<VisualDiff>>(emptyList()) }
    var labels by remember { mutableStateOf(mapOf<String, String>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                pending = session.diff.pendingForExit(exitInspectionId)
                labels = session.repository.roomsFor(exitInspectionId)
                    .associate { it.id to it.label }
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    LaunchedEffect(exitInspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.diffTitle)
        BodyText(Strings.diffHint)
        ErrorText(error)
        VaultButton(
            text = if (busy) Strings.diffRunning else Strings.diffRun,
            onClick = {
                busy = true
                scope.launch {
                    try {
                        session.diff.runDiff(exitInspectionId)
                        refresh()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
        )
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pending.isEmpty()) {
                item(key = "empty") {
                    Text(Strings.diffEmpty, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(pending, key = { it.id }) { row ->
                    DiffRow(
                        row = row,
                        roomLabel = labels[row.exitRoomId] ?: row.exitRoomId.takeLast(6),
                        onReviewed = {
                            scope.launch {
                                try {
                                    session.diff.markReviewed(row.id)
                                    refresh()
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                }
                            }
                        },
                        onDismiss = {
                            scope.launch {
                                try {
                                    session.diff.dismiss(row.id)
                                    refresh()
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                }
                            }
                        },
                    )
                }
            }
        }
        VaultButton(text = Strings.back, onClick = onBack)
    }
}

@Composable
private fun DiffRow(
    row: VisualDiff,
    roomLabel: String,
    onReviewed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val signals = parseDiffSignals(row.signals).joinToString { diffSignalLabel(it) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$roomLabel · ${shotLabel(vault.local.ShotCategory.valueOf(row.shotCategory))}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(signals, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onReviewed) { Text(Strings.diffReviewed) }
            TextButton(onClick = onDismiss) { Text(Strings.diffDismiss) }
        }
    }
}

private fun diffSignalLabel(signal: DiffSignal): String = when (signal) {
    DiffSignal.PRESENCE_CHANGE -> "foto só de um lado"
    DiffSignal.FINDINGS_CHANGED -> "achados mudaram"
    DiffSignal.USER_TAGGED -> "você marcou mudança"
    DiffSignal.LOCATION_CHANGED -> "localização mudou"
    DiffSignal.BYTES_DIFFER -> "bytes diferem"
}
