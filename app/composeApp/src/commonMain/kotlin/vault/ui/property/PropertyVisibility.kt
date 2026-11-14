package vault.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import vault.ui.BrandWhite
import vault.ui.LocalHabimetriaDarkTheme
import vault.ui.StatusBadge

// U09 visibility: archived Properties leave active pickers/lists and
// stay reachable through the Arquivados filter with restore. Restore
// keeps the stable Property ID; drafts stay resumable either way.
const val VISIBILITY_ACTIVE = "ACTIVE"
const val VISIBILITY_ARCHIVED = "ARCHIVED"

// Explicit Ativos/Arquivados filter for the Imóveis list. Persists
// across rotation via the caller's saveable state.
@Composable
fun VisibilityFilterRow(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalHabimetriaDarkTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(if (dark) colors.surfaceContainerHighest else colors.inverseSurface)
            .padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VisibilitySegment(
            selected = selected == VISIBILITY_ACTIVE,
            onClick = { onSelect(VISIBILITY_ACTIVE) },
            label = PropertyStrings.visibilityActive,
            icon = Icons.Filled.Home,
        )
        VisibilitySegment(
            selected = selected == VISIBILITY_ARCHIVED,
            onClick = { onSelect(VISIBILITY_ARCHIVED) },
            label = PropertyStrings.visibilityArchived,
            icon = Icons.Filled.Archive,
        )
    }
}

@Composable
private fun RowScope.VisibilitySegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalHabimetriaDarkTheme.current
    val background = when {
        dark && selected -> colors.primaryContainer
        dark -> colors.surfaceContainerHighest
        selected -> BrandWhite
        else -> colors.inverseSurface
    }
    val ink = when {
        dark && selected -> colors.primary
        dark -> colors.onSurfaceVariant
        selected -> colors.inverseSurface
        else -> BrandWhite
    }
    Surface(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = background,
        contentColor = ink,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (selected) Icons.Filled.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        }
    }
}

// Non-color cue that a row is archived (shape + text, never green alone).
@Composable
fun ArchivedBadge() {
    StatusBadge(PropertyStrings.archivedBadge, Icons.Filled.Archive)
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
