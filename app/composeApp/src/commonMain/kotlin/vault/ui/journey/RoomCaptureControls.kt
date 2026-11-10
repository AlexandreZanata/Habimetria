package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.LocalSession
import vault.local.LocationClaim
import vault.local.ShotCategory
import vault.local.db.LocalEvidence
import vault.local.db.LocalRoom
import vault.rooms.RoomCoverage
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.claimSummary

// Capture, category coverage and saved photos use separate outlined
// sections. Navigation remains secondary and all content scrolls.
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
    RoomCaptureCard(selected, busy, onSelect, onCapture)
    ErrorText(error)
    lastClaim?.let { claim ->
        Text(
            "${Strings.locationLabel}: " + claimSummary(claim.status.name, claim.accuracyM, claim.fixAgeMs, claim.isMocked),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RoomCoverageCard(coverage, na, enabled = !busy, onToggleNa)
    if (evidences.isNotEmpty()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${JourneyStrings.photos} (${evidences.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                evidences.forEach { evidence ->
                    EvidencePhotoRow(
                        session = session,
                        evidence = evidence,
                        onView = { onViewPhoto(evidence) },
                        onDelete = { onDeleteEvidence(evidence.id) },
                    )
                }
            }
        }
    }
    nextRoom?.let { next ->
        CaptureNavigationAction("${Strings.nextRoom}: ${next.label}", !busy) { onOpenRoom(next.id, next.label) }
    }
    CaptureNavigationAction(JourneyStrings.reviewPhotos, !busy, onReview)
}

@Composable
private fun CaptureNavigationAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
