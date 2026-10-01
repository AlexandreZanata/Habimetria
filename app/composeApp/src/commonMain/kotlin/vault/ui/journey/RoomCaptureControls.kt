package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.LocalSession
import vault.local.LocationClaim
import vault.local.ShotCategory
import vault.local.db.LocalEvidence
import vault.local.db.LocalRoom
import vault.rooms.RoomCoverage
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.claimSummary
import vault.ui.shotLabel

// Capture controls: shutter (busy-guarded against double taps),
// category chips, coverage state and the per-photo list. The primary
// task actions (next room, review) close the column; hierarchical
// Back stays in the shared header.
@Composable
fun ColumnScope.RoomCaptureControls(
    session: LocalSession,
    evidences: List<LocalEvidence>,
    nextRoom: LocalRoom?,
    coverage: RoomCoverage?,
    na: Set<ShotCategory>,
    selected: ShotCategory,
    onSelect: (ShotCategory) -> Unit,
    busy: Boolean,
    error: String?,
    lastClaim: LocationClaim?,
    onToggleNa: (ShotCategory) -> Unit,
    onCapture: () -> Unit,
    onDeleteEvidence: (evidenceId: String) -> Unit,
    onViewPhoto: (evidence: LocalEvidence) -> Unit,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onReview: () -> Unit,
) {
    VaultButton(
        text = if (busy) Strings.saving else Strings.photograph,
        onClick = onCapture,
        enabled = !busy,
    )
    Text("${Strings.captureIn}:")
    ShotCategory.entries.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { category ->
                val done = coverage?.captured?.contains(category.name) == true
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text((if (done) "✓ " else "") + shotLabel(category)) },
                )
            }
        }
    }
    ErrorText(error)
    lastClaim?.let { claim ->
        Text(
            "${Strings.locationLabel}: " + claimSummary(
                claim.status.name, claim.accuracyM, claim.fixAgeMs, claim.isMocked,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val percent = coverage?.percent() ?: 0
    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
    Text("${Strings.coverage}: $percent%")
    Text("${Strings.missingShots}:")
    val doneSet = coverage?.captured?.mapNotNull { ShotCategory.parse(it) }?.toSet() ?: emptySet()
    val byCategory = evidences.groupBy { it.shotCategory }
    ShotCategory.entries.forEach { category ->
        val state = when {
            category in na -> "⊘ ${shotLabel(category)}"
            category in doneSet -> "✓ ${shotLabel(category)}"
            else -> "○ ${shotLabel(category)}"
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (category !in doneSet) {
                TextButton(onClick = { onToggleNa(category) }) {
                    Text(if (category in na) "✓ ${Strings.notApplicable}" else Strings.notApplicable)
                }
            }
        }
        byCategory[category.name].orEmpty().forEach { evidence ->
            EvidencePhotoRow(
                session = session,
                evidence = evidence,
                onView = { onViewPhoto(evidence) },
                onDelete = { onDeleteEvidence(evidence.id) },
            )
        }
    }
    val ungrouped = evidences.filter { ShotCategory.parse(it.shotCategory ?: "") == null }
    if (ungrouped.isNotEmpty()) {
        Text("${Strings.evidenceList} (${ungrouped.size})")
        ungrouped.forEach { evidence ->
            EvidencePhotoRow(
                session = session,
                evidence = evidence,
                onView = { onViewPhoto(evidence) },
                onDelete = { onDeleteEvidence(evidence.id) },
            )
        }
    }
    nextRoom?.let { next ->
        VaultButton(
            text = "${Strings.nextRoom}: ${next.label} →",
            onClick = { onOpenRoom(next.id, next.label) },
        )
    }
    VaultButton(text = "${Strings.review} →", onClick = onReview)
}
