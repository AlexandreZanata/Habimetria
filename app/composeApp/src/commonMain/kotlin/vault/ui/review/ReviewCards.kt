package vault.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalRoom
import vault.local.LocalSession
import vault.ui.RecordPanel
import vault.ui.SkeletonLine
import vault.ui.SkeletonThumbnail
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

// Loading placeholders in the same shapes: status card, one room with two
// evidence rows, and one option row. Decorative; announcement stays with
// ProgressStatus on the screen.
@Composable
fun ReviewStatusSkeleton() {
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        .clearAndSetSemantics {},
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonLine(0.7f, 22.dp)
                    SkeletonLine(0.9f)
                }
            }
            SkeletonLine(1f, 48.dp)
        }
    }
}

@Composable
fun ReviewRoomSkeleton() {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonLine(0.4f, 20.dp)
            repeat(2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonThumbnail(64.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonLine(0.7f)
                        SkeletonLine(0.5f)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewOptionSkeleton() {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .clearAndSetSemantics {},
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonLine(0.6f, 20.dp)
                SkeletonLine(0.95f)
            }
        }
    }
}
