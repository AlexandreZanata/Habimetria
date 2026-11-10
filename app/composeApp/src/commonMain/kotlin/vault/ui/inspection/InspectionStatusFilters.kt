package vault.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.STATUS_DRAFT
import vault.local.STATUS_SEALED
import vault.ui.BrandWhite
import vault.ui.LocalHabimetriaDarkTheme
import vault.ui.property.PropertyStrings

private val filters = listOf(
    "ALL" to PropertyStrings.filterAll,
    STATUS_DRAFT to PropertyStrings.filterDraft,
    STATUS_SEALED to PropertyStrings.filterSealed,
)

@Composable
internal fun InspectionStatusFilters(selected: String, onSelect: (String) -> Unit) {
    val dark = LocalHabimetriaDarkTheme.current
    val pine = MaterialTheme.colorScheme.inverseSurface
    val colors = if (dark) {
        FilterChipDefaults.filterChipColors()
    } else {
        FilterChipDefaults.filterChipColors(
            containerColor = pine,
            labelColor = BrandWhite,
            selectedContainerColor = BrandWhite,
            selectedLabelColor = pine,
            selectedLeadingIconColor = pine,
        )
    }
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (status, label) ->
            val active = selected == status
            FilterChip(
                selected = active,
                onClick = { onSelect(status) },
                label = { Text(label) },
                colors = colors,
                border = if (dark) {
                    FilterChipDefaults.filterChipBorder(enabled = true, selected = active)
                } else {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = active, borderColor = pine, selectedBorderColor = pine,
                    )
                },
                leadingIcon = if (active) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }
    }
}
