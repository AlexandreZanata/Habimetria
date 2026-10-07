package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
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
import vault.ui.report.PdfJourneySegments
import vault.ui.report.PdfOptionCard
import vault.ui.report.PdfSectionDivider
import vault.ui.report.PdfSectionHeader
import vault.ui.review.ReviewRoomCard
import vault.ui.review.ReviewStatusCard

// LocalReviewScreen in the Relatório PDF style: segmented Cômodos/Revisão
// shortcuts, a green status card with the seal action, room cards with
// every evidence and finding, then option cards for report data,
// assistant and entry-vs-exit compare. Sealing arrives in L5; this screen
// never seals.
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
        ScreenTitle(Strings.review)
        PdfJourneySegments(onRooms = onRoomsShortcut, onReview = {}, activeReview = true)
        ErrorText(error)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReviewStatusCard(
                sealed = sealed,
                photoCount = evidences.size,
                roomCount = rooms.size,
                sealEnabled = true,
                onSeal = onSeal,
            )
            PdfSectionHeader(Strings.rooms, Strings.reviewRoomsBody)
            rooms.forEach { room ->
                ReviewRoomCard(
                    session = session,
                    room = room,
                    evidences = evidences.filter { it.roomId == room.id },
                    findings = findings,
                    percent = percents[room.id] ?: 0,
                    sealed = sealed,
                    onIssue = { sheetFor = it },
                    onEvidenceChanged = { refresh() },
                )
            }
            PdfSectionDivider()
            PdfSectionHeader(Strings.pdfMoreOptions, Strings.reviewMoreOptionsBody)
            PdfOptionCard(
                Icons.Filled.Description,
                Strings.reviewReportData,
                Strings.reviewReportDataBody,
                enabled = true,
                onClick = { reportDetails = true },
            )
            PdfOptionCard(
                Icons.Filled.AutoAwesome,
                Strings.inspectorTitle,
                Strings.reviewAssistantBody,
                enabled = true,
                onClick = onInspector,
            )
            if (kind == "exit") {
                PdfOptionCard(
                    Icons.Filled.CompareArrows,
                    Strings.compareTitle,
                    Strings.reviewCompareBody,
                    enabled = true,
                    onClick = onCompare,
                )
            }
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
