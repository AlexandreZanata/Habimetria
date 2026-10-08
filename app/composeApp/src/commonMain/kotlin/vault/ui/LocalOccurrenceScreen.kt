package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import vault.local.LocalSession
import vault.local.db.LocalOccurrence
import vault.local.newLocalId

// Occurrence timeline per property: post-seal story that never mutates
// any inspection. Title required, body optional, photos attached
// capture-first with the same store-then-record discipline.
@Composable
fun LocalOccurrenceScreen(
    session: LocalSession,
    photoCapture: PhotoCapture,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var propertyId by remember { mutableStateOf<String?>(null) }
    var occurrences by remember { mutableStateOf<List<LocalOccurrence>?>(null) }
    var counts by remember { mutableStateOf(mapOf<String, Int>()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                val property = session.occurrences.propertyForInspection(inspectionId)
                propertyId = property
                val list = session.occurrences.listForProperty(property)
                occurrences = list
                counts = list.associate { it.id to session.occurrences.attachmentsFor(it.id).size }
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.occurrenceTitle)
        BodyText(Strings.occurrenceSealedNote)
        ErrorText(error)
        VaultTextField(value = title, onValueChange = { title = it }, label = Strings.occurrenceTitleHint)
        VaultTextField(
            value = body,
            onValueChange = { body = it },
            label = Strings.occurrenceBodyHint,
            singleLine = false,
        )
        VaultButton(
            text = Strings.newOccurrence,
            onClick = {
                val property = propertyId ?: return@VaultButton
                scope.launch {
                    try {
                        session.occurrences.createOccurrence(property, title, body.ifBlank { null })
                        title = ""
                        body = ""
                        refresh()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
            enabled = !busy && title.trim().isNotEmpty(),
        )
        val list = occurrences.orEmpty()
        if (occurrences == null) {
            ProgressStatus(true)
            SkeletonLine(0.5f, 20.dp)
            SkeletonLine(0.3f)
            SkeletonLine(0.5f, 20.dp)
            SkeletonLine(0.3f)
        } else if (list.isEmpty()) {
            BodyText(Strings.occurrenceEmpty)
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { occurrence ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(occurrence.title, style = MaterialTheme.typography.titleMedium)
                    occurrence.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text(
                        "${counts[occurrence.id] ?: 0} ${Strings.photosSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    VaultButton(
                        text = if (busy) Strings.saving else Strings.occurrenceAttach,
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val shot = when (val result = photoCapture.captureDetailed()) {
                                        is CaptureResult.Failure -> {
                                            error = captureErrorString(result.reason)
                                            return@launch
                                        }
                                        is CaptureResult.Photo -> result
                                    }
                                    val attachmentId = newLocalId("att")
                                    val stored = withContext(Dispatchers.Default) {
                                        session.originals.storeOriginal(attachmentId, shot.bytes)
                                    }
                                    withContext(Dispatchers.Default) {
                                        session.occurrences.attachPhoto(
                                            occurrence.id, "$attachmentId.bin", shot.mime,
                                            stored.size, stored.sha256, attachmentId = attachmentId,
                                        )
                                    }
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
                }
            }
            // Same in-flow rule as rooms: footers pinned after a weighted
            // list ride above the keyboard instead of staying put.
            item {
                VaultButton(text = Strings.back, onClick = onBack)
            }
        }
    }
}
