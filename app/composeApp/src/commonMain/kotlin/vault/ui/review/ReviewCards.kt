package vault.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalRoom
import vault.local.LocalSession
import vault.ui.Strings
import vault.ui.journey.OrientationCorrectionRow
import vault.ui.journey.ReviewEvidenceRow

// Review status card in the Relatório PDF style: pale-green card, check
// badge, photo/room counts and the primary seal action. Sealed shows the
// frozen notice instead of the button; nothing else is dropped.
@Composable
fun ReviewStatusCard(
    sealed: Boolean,
    photoCount: Int,
    roomCount: Int,
    sealEnabled: Boolean,
    onSeal: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (sealed) Strings.reviewSealedTitle else Strings.reviewStatusTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        if (sealed) Strings.sealedLocked
                        else "$photoCount ${Strings.photosSuffix} · $roomCount ${Strings.roomsSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (!sealed) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "$photoCount ${Strings.photosSuffix}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (!sealed) {
                Button(onClick = onSeal, enabled = sealEnabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.sealNow)
                }
            }
        }
    }
}

// One room as an outlined card: title with coverage plus its evidence
// rows with finding counts and orientation rows. Read-mostly; findings
// still edit while in DRAFT through the existing sheet.
@Composable
fun ReviewRoomCard(
    session: LocalSession,
    room: LocalRoom,
    evidences: List<LocalEvidence>,
    findings: List<LocalFinding>,
    percent: Int,
    sealed: Boolean,
    onIssue: (LocalEvidence) -> Unit,
    onEvidenceChanged: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${room.label} · $percent%",
                style = MaterialTheme.typography.titleMedium,
            )
            evidences.forEach { evidence ->
                ReviewEvidenceRow(
                    session = session,
                    evidence = evidence,
                    findingCount = findings.count { it.evidenceId == evidence.id },
                    onIssue = if (sealed) null else ({ onIssue(evidence) }),
                )
                OrientationCorrectionRow(session, evidence, sealed, onChanged = onEvidenceChanged)
            }
        }
    }
}
