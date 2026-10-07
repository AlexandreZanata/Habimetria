package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// U09 visibility: archived Properties leave active pickers/lists and
// stay reachable through the Arquivados filter with restore. Restore
// keeps the stable Property ID; drafts stay resumable either way.
const val VISIBILITY_ACTIVE = "ACTIVE"
const val VISIBILITY_ARCHIVED = "ARCHIVED"

// Explicit Ativos/Arquivados filter for the Imóveis list. Persists
// across rotation via the caller's saveable state.
@Composable
fun VisibilityFilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == VISIBILITY_ACTIVE,
            onClick = { onSelect(VISIBILITY_ACTIVE) },
            label = { Text(PropertyStrings.visibilityActive) },
        )
        FilterChip(
            selected = selected == VISIBILITY_ARCHIVED,
            onClick = { onSelect(VISIBILITY_ARCHIVED) },
            label = { Text(PropertyStrings.visibilityArchived) },
        )
    }
}

// Non-color cue that a row is archived (shape + text, never green alone).
@Composable
fun ArchivedBadge() {
    Text(
        PropertyStrings.archivedBadge,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Non-destructive by design: no confirmation dialog, busy-guarded
// against double activation, errors surface with retry on next tap.
@Composable
fun RestoreButton(busy: Boolean, onRestore: () -> Unit) {
    OutlinedButton(
        onClick = onRestore,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(PropertyStrings.restoreAction) }
}
