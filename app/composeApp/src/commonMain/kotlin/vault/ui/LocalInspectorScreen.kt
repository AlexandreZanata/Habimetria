package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import vault.local.InspectorGap
import vault.local.LocalSession
import vault.local.LocalValidationException
import vault.local.STATUS_DRAFT
import vault.local.db.AiSuggestion
import vault.local.db.LocalEvidence
import vault.ui.inspector.InspectorGapCard
import vault.ui.inspector.InspectorPhotoCard
import vault.ui.inspector.InspectorStrings
import vault.ui.journey.PhotoViewer

private data class InspectorViewData(
    val editable: Boolean,
    val gaps: List<InspectorGap>,
    val coverageReminders: Map<String, List<AiSuggestion>>,
    val photoReminders: Map<String, List<AiSuggestion>>,
    val roomLabels: Map<String, String>,
    val photos: Map<String, LocalEvidence>,
)

@Composable
fun LocalInspectorScreen(session: LocalSession, inspectionId: String) {
    val scope = rememberCoroutineScope()
    var data by remember(inspectionId) { mutableStateOf<InspectorViewData?>(null) }
    var busy by remember(inspectionId) { mutableStateOf(false) }
    var runningCheck by remember(inspectionId) { mutableStateOf(false) }
    var error by remember(inspectionId) { mutableStateOf<String?>(null) }
    var viewing by remember(inspectionId) { mutableStateOf<LocalEvidence?>(null) }

    suspend fun refresh() {
        val repository = session.repository
        val inspection = repository.getInspection(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        val (coverage, photos) = session.inspector.pendingForInspection(inspectionId)
            .partition { it.evidenceId.isBlank() }
        data = InspectorViewData(
            editable = inspection.status == STATUS_DRAFT,
            gaps = session.inspector.coverageGaps(inspectionId),
            coverageReminders = coverage.groupBy { it.roomId },
            photoReminders = photos.groupBy { it.evidenceId },
            roomLabels = repository.roomsFor(inspectionId).associate { it.id to it.label },
            photos = repository.evidencesFor(inspectionId).associateBy { it.id },
        )
    }

    fun perform(checking: Boolean = false, work: suspend () -> Unit = {}) {
        if (busy) return
        busy = true
        runningCheck = checking
        error = null
        scope.launch {
            try {
                work()
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
                runningCheck = false
            }
        }
    }

    LaunchedEffect(session, inspectionId) { perform() }

    val current = data
    val editable = current?.editable == true
    val showLoading = rememberLoadingVisible(current == null && error == null)
    val ignore: (String) -> Unit = { id -> perform { session.inspector.ignore(id) } }

    ScreenColumn {
        ErrorText(error)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "overview") {
                RecordPanel(
                    icon = Icons.AutoMirrored.Outlined.FactCheck,
                    title = InspectorStrings.title,
                    body = if (current?.editable == false) InspectorStrings.readOnly else InspectorStrings.intro,
                    action = if (!editable) null else if (runningCheck) InspectorStrings.running else InspectorStrings.run,
                    actionEnabled = !busy,
                    onAction = { perform(checking = true) { session.inspector.runInspection(inspectionId) } },
                )
            }
            if (showLoading) item(key = "loading") { ProgressStatus(true) }
            if (error != null && current == null) {
                item(key = "retry") {
                    OutlinedButton(onClick = { perform() }, enabled = !busy) { Text(Strings.retry) }
                }
            }
            if (current != null) {
                if (current.gaps.isNotEmpty()) {
                    item(key = "gaps") { InspectorHeading(InspectorStrings.gaps) }
                    items(current.gaps, key = { "gap-${it.roomId}" }) { gap ->
                        InspectorGapCard(gap, current.coverageReminders[gap.roomId].orEmpty(), editable, busy, ignore)
                    }
                }
                item(key = "suggestions") { InspectorHeading(InspectorStrings.suggestions) }
                if (current.photoReminders.isEmpty()) {
                    item(key = "empty") {
                        RecordPanel(Icons.Outlined.Photo, InspectorStrings.emptyTitle, InspectorStrings.emptyBody)
                    }
                }
                items(current.photoReminders.entries.toList(), key = { "photo-${it.key}" }) { (id, hints) ->
                    InspectorPhotoCard(
                        photo = current.photos[id],
                        roomLabel = current.roomLabels[hints.first().roomId],
                        hints = hints,
                        editable = editable,
                        busy = busy,
                        onPreview = { viewing = it },
                        onConfirm = { hintId -> perform { session.inspector.confirm(hintId) } },
                        onIgnore = ignore,
                    )
                }
            }
        }
    }

    viewing?.let { PhotoViewer(session, it, onClose = { viewing = null }) }
}

@Composable
private fun InspectorHeading(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}
