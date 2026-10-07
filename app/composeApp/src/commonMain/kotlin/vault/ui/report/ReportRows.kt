package vault.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vault.local.ReportOverview
import vault.local.formatSealedAtDevice
import vault.ui.Strings
import vault.ui.property.PropertyStrings

// Finalized report card: version title plus single-line open/verify
// entries and a red delete action that confirms through
// ReportDeleteHost before touching anything.
@Composable
fun FinalizedRow(
    overview: ReportOverview,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${ReportStrings.versionTitle} v${overview.report.versionNumber} · " +
                    formatSealedAtDevice(
                        overview.report.completedAtDevice ?: overview.report.createdAtDevice,
                    ),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text(PropertyStrings.openDetail, maxLines = 1)
                }
                OutlinedButton(onClick = onVerify, modifier = Modifier.weight(1f)) {
                    Text(ReportStrings.verifyShort, maxLines = 1)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = ReportStrings.deleteDescription,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// Sealed inspection awaiting its PDF: one clear generation entry,
// card-styled like the finalized rows above it.
@Composable
fun PendingRow(onGenerate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) { Text(Strings.generatePdf, maxLines = 1) }
    }
}
