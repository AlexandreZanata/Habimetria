package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.launch
import vault.local.LocalSession
import vault.local.STATUS_SEALED
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalRoom
import vault.ui.journey.FindingInputSheet
import vault.ui.journey.JourneyShortcuts
import vault.ui.journey.ReviewEvidenceRow

// LocalReviewScreen: the sealed-truth preview while in DRAFT — every
// room with its label unchanged, every evidence, every finding and the
// honest coverage gaps. Sealing arrives in L5; this screen never seals.
@Composable
fun LocalReviewScreen(
    container: AppContainer,
    session: LocalSession,
    inspectionId: String,
    onSeal: () -> Unit,
    onCompare: () -> Unit,
    onInspector: () -> Unit,
    onRoomsShortcut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<LocalRoom>>(emptyList()) }
    var evidences by remember { mutableStateOf<List<LocalEvidence>>(emptyList()) }
    var findings by remember { mutableStateOf<List<LocalFinding>>(emptyList()) }
    var percents by remember { mutableStateOf(mapOf<String, Int>()) }
    var sheetFor by remember { mutableStateOf<LocalEvidence?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("entry") }
    var reportDetails by remember { mutableStateOf(false) }
    var sealed by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try {
                val inspection = session.repository.getInspection(inspectionId)
                sealed = inspection?.status == STATUS_SEALED
                kind = inspection?.kind ?: "entry"
                val roomList = session.repository.roomsFor(inspectionId)
                rooms = roomList
                evidences = session.repository.evidencesFor(inspectionId)
                findings = roomList.flatMap { session.repository.findingsForRoom(it.id) }
                percents = roomList.associate { it.id to session.coverage.roomCoverage(it.id).percent() }
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle("${Strings.review} — ${evidences.size} ${Strings.photosSuffix}")
        JourneyShortcuts(onRooms = onRoomsShortcut, onReview = null)
        ErrorText(error)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rooms, key = { it.id }) { room ->
                val roomEvidence = evidences.filter { it.roomId == room.id }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${room.label} · ${percents[room.id] ?: 0}%",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    roomEvidence.forEach { evidence ->
                        ReviewEvidenceRow(
                            session = session,
                            evidence = evidence,
                            findingCount = findings.count { it.evidenceId == evidence.id },
                            onIssue = if (sealed) null else ({ sheetFor = evidence }),
                        )
                        vault.ui.journey.OrientationCorrectionRow(session, evidence, sealed, onChanged = { refresh() })
                    }
                }
            }
        }
        VaultButton(text = "Dados do relatório", onClick = { reportDetails = true })
        if (sealed) {
            Text(Strings.sealedLocked, style = MaterialTheme.typography.bodyMedium)
        } else {
            VaultButton(text = "${Strings.sealNow} →", onClick = onSeal)
        }
        VaultButton(text = "${Strings.inspectorTitle} →", onClick = onInspector)
        if (kind == "exit") {
            VaultButton(text = "${Strings.compareTitle} →", onClick = onCompare)
        }
    }

    if (reportDetails) ReportDetailsSheet(session, inspectionId, sealed, container.filePicker) { reportDetails = false }

    sheetFor?.let { evidence ->
        val roomId = evidence.roomId
        if (roomId != null) {
            FindingInputSheet(
                session = session,
                roomId = roomId,
                evidenceId = evidence.id,
                onDone = { sheetFor = null; refresh() },
                onDismiss = { sheetFor = null },
            )
        }
    }
}
