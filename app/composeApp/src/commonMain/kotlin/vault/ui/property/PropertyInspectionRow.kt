package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.STATUS_SEALED
import vault.local.db.LocalInspection
import vault.local.formatSealedAtDevice
import vault.ui.Strings
import vault.ui.inspection.InspectionStatusBadge
import vault.ui.inspection.InspectionStrings

@Composable
internal fun InspectionRow(
    inspection: LocalInspection,
    busy: Boolean,
    onResume: () -> Unit,
    onReview: () -> Unit,
) {
    val sealed = inspection.status == STATUS_SEALED
    val colors = MaterialTheme.colorScheme
    val title = when (inspection.kind) {
        "exit" -> InspectionStrings.kindExit
        "technical" -> InspectionStrings.kindTechnical
        else -> InspectionStrings.kindEntry
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = colors.primary, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatSealedAtDevice(inspection.createdAtDevice),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            InspectionStatusBadge(sealed)
            HorizontalDivider(color = colors.outlineVariant)
            TextButton(
                onClick = if (sealed) onReview else onResume,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (sealed) Strings.review else Strings.continueAction, Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
