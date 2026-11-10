package vault.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.ReportOverview
import vault.local.formatSealedAtDevice
import vault.ui.Strings

@Composable
fun FinalizedRow(
    overview: ReportOverview,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp).padding(10.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${ReportStrings.versionTitle} v${overview.report.versionNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                    )
                    Text(
                        formatSealedAtDevice(overview.report.completedAtDevice ?: overview.report.createdAtDevice),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.errorContainer,
                        contentColor = colors.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = ReportStrings.deleteDescription)
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ),
                ) {
                    Text(ReportStrings.openPdf)
                }
                OutlinedButton(
                    onClick = onVerify,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(ReportStrings.verifyShort)
                }
            }
        }
    }
}

// Generation entry for a sealed inspection awaiting its PDF.
@Composable
fun PendingRow(onGenerate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) { Text(Strings.generatePdf, maxLines = 1) }
    }
}
