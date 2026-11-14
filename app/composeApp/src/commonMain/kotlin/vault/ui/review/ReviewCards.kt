package vault.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalRoom
import vault.local.LocalSession
import vault.ui.RecordPanel
import vault.ui.Strings
import vault.ui.journey.OrientationCorrectionRow
import vault.ui.journey.ReviewEvidenceRow

// The seal action is available only during draft review.
@Composable
fun ReviewStatusCard(
    sealed: Boolean,
    photoCount: Int,
    roomCount: Int,
    sealEnabled: Boolean,
    onSeal: () -> Unit,
) {
    RecordPanel(
        icon = if (sealed) Icons.Filled.Lock else Icons.AutoMirrored.Outlined.FactCheck,
        title = if (sealed) Strings.reviewSealedTitle else Strings.reviewStatusTitle,
        body = if (sealed) Strings.sealedLocked
        else "$photoCount ${Strings.photosSuffix} · $roomCount ${Strings.roomsSuffix}",
        action = if (sealed) null else Strings.sealNow,
        actionEnabled = sealEnabled,
        onAction = onSeal,
    )
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
