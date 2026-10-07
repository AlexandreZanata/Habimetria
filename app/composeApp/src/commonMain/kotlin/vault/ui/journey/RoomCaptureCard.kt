package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.ShotCategory
import vault.ui.Strings
import vault.ui.shotLabel

// Wrapping categories stay above the shutter, with a distinct selected
// state. Captured state belongs to the coverage card, not the selection.
@Composable
internal fun RoomCaptureCard(
    selected: ShotCategory,
    busy: Boolean,
    onSelect: (ShotCategory) -> Unit,
    onCapture: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Strings.captureIn, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(JourneyStrings.captureHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShotCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { onSelect(category) },
                        enabled = !busy,
                        label = { Text(shotLabel(category)) },
                        leadingIcon = if (selected == category) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            JourneyPrimaryAction(
                text = if (busy) Strings.saving else Strings.photograph,
                icon = Icons.Filled.CameraAlt,
                onClick = onCapture,
                enabled = !busy,
            )
        }
    }
}
