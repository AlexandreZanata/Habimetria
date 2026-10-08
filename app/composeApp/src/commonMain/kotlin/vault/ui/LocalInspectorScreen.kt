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
import vault.local.InspectorGap
import vault.local.LocalSession
import vault.local.SuggestionKind
import vault.local.db.AiSuggestion

// V1.4 inspector screen: coverage gaps plus possible stain/crack hints.
// Every hint needs a user decision (Confirm creates a Finding, Ignore
// dismisses). Nothing here assigns liability or writes findings alone.
@Composable
fun LocalInspectorScreen(
    session: LocalSession,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var gaps by remember { mutableStateOf<List<InspectorGap>>(emptyList()) }
    var pending by remember { mutableStateOf<List<AiSuggestion>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try {
                gaps = session.inspector.coverageGaps(inspectionId)
                pending = session.inspector.pendingForInspection(inspectionId)
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                loaded = true
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.inspectorTitle)
        BodyText(Strings.inspectorHint)
        ErrorText(error)
        VaultButton(
            text = if (busy) Strings.inspectorRunning else Strings.inspectorRun,
            onClick = {
                busy = true
                scope.launch {
                    try {
                        session.inspector.runInspection(inspectionId)
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
            if (!loaded) {
                item(key = "loading") { ProgressStatus(true) }
                item(key = "skeleton-1") { SkeletonLine(0.5f, 20.dp) }
                item(key = "skeleton-2") { SkeletonLine(0.8f) }
                item(key = "skeleton-3") { SkeletonLine(0.6f) }
                item(key = "skeleton-4") { SkeletonLine(0.8f) }
            } else {
            if (gaps.isNotEmpty()) {
                item(key = "gaps-header") {
                    Text(Strings.inspectorGaps, style = MaterialTheme.typography.titleMedium)
                }
                items(gaps, key = { "gap-${it.roomId}" }) { gap ->
                    Text(
                        "${gap.roomLabel}: ${gap.missing.joinToString { shotLabel(it) }}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (pending.isNotEmpty()) {
                item(key = "pending-header") {
                    Text(Strings.inspectorSuggestions, style = MaterialTheme.typography.titleMedium)
                }
                items(pending, key = { it.id }) { hint ->
                    InspectorHintRow(
                        hint = hint,
                        onConfirm = {
                            scope.launch {
                                try {
                                    session.inspector.confirm(hint.id)
                                    refresh()
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                }
                            }
                        },
                        onIgnore = {
                            scope.launch {
                                try {
                                    session.inspector.ignore(hint.id)
                                    refresh()
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                }
                            }
                        },
                    )
                }
            } else {
                item(key = "empty") {
                    Text(Strings.inspectorEmpty, style = MaterialTheme.typography.bodyMedium)
                }
            }
            }
        }
        VaultButton(text = Strings.back, onClick = onBack)
    }
}

@Composable
private fun InspectorHintRow(
    hint: AiSuggestion,
    onConfirm: () -> Unit,
    onIgnore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val label = when (hint.kind) {
            SuggestionKind.POSSIBLE_STAIN.serial -> Strings.inspectorStain
            SuggestionKind.POSSIBLE_CRACK.serial -> Strings.inspectorCrack
            else -> "${Strings.inspectorGap}: ${hint.detail.lowercase()}"
        }
        val where = if (hint.evidenceId.isNotBlank()) "#${hint.evidenceId.takeLast(6)}" else ""
        Text("$label $where".trim(), style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hint.kind != SuggestionKind.COVERAGE_GAP.serial) {
                TextButton(onClick = onConfirm) { Text(Strings.inspectorConfirm) }
            }
            TextButton(onClick = onIgnore) { Text(Strings.inspectorIgnore) }
        }
    }
}
