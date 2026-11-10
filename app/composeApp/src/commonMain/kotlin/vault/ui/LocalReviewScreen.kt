package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import vault.ui.review.ReviewOptionSkeleton
import vault.ui.review.ReviewRoomCard
import vault.ui.review.ReviewRoomSkeleton
import vault.ui.review.ReviewStatusCard

// Index of the rooms section header in the screen list: status card is 0.
private const val ROOMS_HEADER_INDEX = 1

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
    var loaded by remember { mutableStateOf(false) }

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
            } finally {
                loaded = true
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    // Rooms is a DRAFT-only destination: it redirects sealed inspections
    // back here. So on a sealed inspection the Cômodos segment scrolls to
    // the rooms section on this page instead of bouncing off Rooms.
    val listState = rememberLazyListState()
    val showLoading = rememberLoadingVisible(!loaded)
    fun openRooms() {
        if (sealed) {
            scope.launch { listState.animateScrollToItem(ROOMS_HEADER_INDEX) }
        } else {
            onRoomsShortcut()
        }
    }

    ScreenColumn {
        PdfJourneySegments(onRooms = ::openRooms, onReview = {}, activeReview = true)
        ErrorText(error)
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showLoading) {
                item(key = "loading") {
                    LoadingSkeleton {
                        RecordPanelSkeleton(featured = true)
                        SkeletonLine(0.33f, 20.dp)
                        SkeletonLine(0.58f, 14.dp)
                        ReviewRoomSkeleton()
                        ReviewOptionSkeleton()
                    }
                }
                return@LazyColumn
            } else if (!loaded) {
                // Grace period: data is on its way, paint nothing yet.
                return@LazyColumn
            }
            item(key = "status") {
                ReviewStatusCard(
                    sealed = sealed,
                    photoCount = evidences.size,
                    roomCount = rooms.size,
                    sealEnabled = true,
                    onSeal = onSeal,
                )
            }
            item(key = "rooms-header") {
                PdfSectionHeader(Strings.rooms, Strings.reviewRoomsBody)
            }
            items(rooms, key = { it.id }) { room ->
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
            item(key = "options-divider") { PdfSectionDivider() }
            item(key = "options-header") {
                PdfSectionHeader(Strings.pdfMoreOptions, Strings.reviewMoreOptionsBody)
            }
            item(key = "options-report") {
                PdfOptionCard(
                    Icons.Filled.Description,
                    Strings.reviewReportData,
                    Strings.reviewReportDataBody,
                    enabled = true,
                    onClick = { reportDetails = true },
                )
            }
            item(key = "options-assistant") {
                PdfOptionCard(
                    Icons.Filled.AutoAwesome,
                    Strings.inspectorTitle,
                    Strings.reviewAssistantBody,
                    enabled = true,
                    onClick = onInspector,
                )
            }
            if (kind == "exit") {
                item(key = "options-compare") {
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
