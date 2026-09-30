package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import vault.findings.FindingPreset
import vault.local.ExifGps
import vault.local.LocalSession
import vault.local.db.LocalEvidence
import vault.local.distanceMeters
import vault.local.exifDisagrees
import vault.local.db.LocalFinding
import vault.local.db.LocalRoom

// LocalReviewScreen: the sealed-truth preview while in DRAFT — every
// room with its label unchanged, every evidence, every finding and the
// honest coverage gaps. Sealing arrives in L5; this screen never seals.
@Composable
fun LocalReviewScreen(
    container: AppContainer,
    session: LocalSession,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<LocalRoom>>(emptyList()) }
    var evidences by remember { mutableStateOf<List<LocalEvidence>>(emptyList()) }
    var findings by remember { mutableStateOf<List<LocalFinding>>(emptyList()) }
    var percents by remember { mutableStateOf(mapOf<String, Int>()) }
    var sheetFor by remember { mutableStateOf<LocalEvidence?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                val roomList = session.repository.roomsFor(inspectionId)
                rooms = roomList
                evidences = session.repository.evidencesFor(inspectionId)
                findings = roomList.flatMap { session.repository.findingsForRoom(it.id) }
                percents = roomList.associate { it.id to session.coverage.roomCoverage(it.id).percent() }
                error = null
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle("${Strings.review} — ${evidences.size} ${Strings.photosSuffix}")
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
                        LocalEvidenceRow(
                            session = session,
                            evidence = evidence,
                            findingCount = findings.count { it.evidenceId == evidence.id },
                            onIssue = { sheetFor = evidence },
                        )
                    }
                }
            }
        }
        VaultButton(text = Strings.back, onClick = onBack)
    }

    sheetFor?.let { evidence ->
        val roomId = evidence.roomId
        if (roomId != null) {
            LocalFindingSheet(
                session = session,
                roomId = roomId,
                evidenceId = evidence.id,
                onDone = { sheetFor = null; refresh() },
                onDismiss = { sheetFor = null },
            )
        }
    }
}

@Composable
private fun LocalEvidenceRow(
    session: LocalSession,
    evidence: LocalEvidence,
    findingCount: Int,
    onIssue: () -> Unit,
) {
    val thumb = remember(evidence.id) {
        val path = session.derivatives.ensureForDisplay(
            evidence.id, session.originals.originalPath(evidence.id),
        )
        runCatching { decodeThumbnail(path) }.getOrNull()
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = "${Strings.photosSuffix}: ${evidence.shotCategory}",
                modifier = Modifier.size(72.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("#${evidence.sequence} ${evidence.shotCategory ?: "?"}")
            Text(
                claimSummary(
                    evidence.locationStatus, evidence.accuracyM,
                    evidence.fixAgeMs, evidence.isMocked,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val fixLat = evidence.latitude
            val fixLon = evidence.longitude
            val exifLat = evidence.exifLatitude
            val exifLon = evidence.exifLongitude
            if (fixLat != null && fixLon != null && exifLat != null && exifLon != null &&
                exifDisagrees(fixLat, fixLon, ExifGps(exifLat, exifLon))
            ) {
                val gap = distanceMeters(fixLat, fixLon, exifLat, exifLon).toInt()
                Text("EXIF ⚠ $gap m", style = MaterialTheme.typography.bodyMedium)
            }
            if (findingCount > 0) Text("$findingCount ${Strings.findingsList.lowercase()}")
        }
        Button(onClick = onIssue) { Text(Strings.reportIssue) }
    }
}

// LocalFindingSheet: preset chips + optional note, stored offline.
// Free room names never replace issue classification: presets stay.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalFindingSheet(
    session: LocalSession,
    roomId: String,
    evidenceId: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val scope = rememberCoroutineScope()
        var preset by remember { mutableStateOf(FindingPreset.STAIN) }
        var note by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(Strings.whatFound)
            FindingPreset.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { preset = p },
                            label = { Text(p.name.lowercase()) },
                        )
                    }
                }
            }
            VaultTextField(value = note, onValueChange = { note = it }, label = Strings.noteOptional, singleLine = false)
            ErrorText(error)
            VaultButton(
                text = if (busy) Strings.savingFinding else Strings.saveFinding,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            session.repository.addFinding(roomId, evidenceId, preset, note.ifBlank { null })
                            onDone()
                        } catch (e: Exception) {
                            error = e.message?.take(160)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            )
        }
    }
}
