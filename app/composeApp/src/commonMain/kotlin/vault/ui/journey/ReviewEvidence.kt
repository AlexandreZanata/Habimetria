package vault.ui.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.findings.FindingPreset
import vault.findings.label
import vault.local.ExifGps
import vault.local.LocalSession
import vault.local.distanceMeters
import vault.local.exifDisagrees
import vault.local.db.LocalEvidence
import vault.ui.ErrorText
import vault.ui.ScreenTitle
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.VaultTextField
import vault.ui.claimSummary
import vault.ui.decodeThumbnail
import vault.ui.userErrorMessage

// One evidence row in review: thumbnail, sequence, location claim and
// finding count. The issue entry stays available until sealed.
@Composable
fun ReviewEvidenceRow(
    session: LocalSession,
    evidence: LocalEvidence,
    findingCount: Int,
    onIssue: (() -> Unit)?,
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
        if (onIssue != null) {
            Button(onClick = onIssue) { Text(Strings.reportIssue) }
        }
    }
}

// Finding input: preset chips + optional note, stored offline. The
// selected preset and the typed note survive rotation (saveable) so a
// tab switch or resize never loses a half-written finding. Free room
// names never replace issue classification: presets stay.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingInputSheet(
    session: LocalSession,
    roomId: String,
    evidenceId: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val scope = rememberCoroutineScope()
        var presetName by rememberSaveable { mutableStateOf(FindingPreset.STAIN.name) }
        var note by rememberSaveable { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val preset = FindingPreset.valueOf(presetName)
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
                            onClick = { presetName = p.name },
                            label = { Text(p.label()) },
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
}

