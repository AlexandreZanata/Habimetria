package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.ShotCategory
import vault.rooms.RoomCoverage
import vault.ui.Strings
import vault.ui.shotLabel

// Category coverage is read from the repository. N/A is an explicit
// exclusion, never a captured photo or a fabricated confirmation.
@Composable
internal fun RoomCoverageCard(
    coverage: RoomCoverage?,
    na: Set<ShotCategory>,
    enabled: Boolean,
    onToggleNa: (ShotCategory) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    JourneyStrings.captureOverview,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    coverage?.let { "${it.percent()}%" } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (coverage == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            } else {
                LinearProgressIndicator(
                    progress = { coverage.percent() / 100f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                coverage?.let { JourneyStrings.covered(it.captured.size, it.required.size) } ?: Strings.loading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                ShotCategory.entries.forEachIndexed { index, category ->
                    CoverageCategoryRow(
                        category, category.name in coverage?.captured.orEmpty(), category in na,
                        loading = coverage == null, enabled = enabled && coverage != null,
                        onToggle = { onToggleNa(category) },
                    )
                    if (index < ShotCategory.entries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Text(JourneyStrings.naHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoverageCategoryRow(
    category: ShotCategory,
    captured: Boolean,
    excluded: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val label = shotLabel(category)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when {
                excluded -> Icons.Outlined.RemoveCircleOutline
                captured -> Icons.Filled.CheckCircle
                else -> Icons.Outlined.Circle
            },
            contentDescription = null,
            tint = if (captured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    loading -> Strings.loading
                    excluded -> JourneyStrings.excluded
                    captured -> JourneyStrings.captured
                    else -> JourneyStrings.pending
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!captured) {
            TextButton(
                onClick = onToggle,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (excluded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    selected = excluded
                    contentDescription = JourneyStrings.naAction(label, excluded)
                },
            ) { Text(Strings.notApplicable) }
        }
    }
}
