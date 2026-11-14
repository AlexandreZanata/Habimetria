package vault.ui.inspection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.InspectionOverview
import vault.local.STATUS_SEALED
import vault.ui.StatusBadge
import vault.ui.Strings
import vault.ui.property.PropertyStrings

// One card per inspection: property name as the title, a colored status
// badge (icon plus text, never color alone) and the options menu.
// The grouping headers are gone because every card names its property.
@Composable
fun InspectionRow(
    overview: InspectionOverview,
    busy: Boolean,
    onResume: () -> Unit,
    onReview: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val sealed = overview.inspection.status == STATUS_SEALED
    val kind = when (overview.inspection.kind) {
        "exit" -> InspectionStrings.kindExit
        "technical" -> InspectionStrings.kindTechnical
        else -> InspectionStrings.kindEntry
    }
    val title = PropertyStrings.archivedSuffix(
        overview.propertyLabel.ifBlank { InspectionStrings.unknownProperty },
        overview.propertyArchived,
    )
    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { menu = true },
                        enabled = !busy,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "${InspectionStrings.optionsDescription}: $title"
                        },
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val colors = MaterialTheme.colorScheme
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        modifier = Modifier.widthIn(min = 160.dp),
                        shape = MaterialTheme.shapes.medium,
                        containerColor = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    PropertyStrings.removeShort,
                                    modifier = Modifier.semantics {
                                        contentDescription = "${PropertyStrings.removeShort}: $title"
                                    },
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            colors = MenuDefaults.itemColors(
                                textColor = colors.error,
                                leadingIconColor = colors.error,
                            ),
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    kind,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                InspectionStatusBadge(sealed = sealed)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(
                onClick = if (sealed) onReview else onResume,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (sealed) Strings.reviewShort else Strings.continueAction,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun InspectionStatusBadge(sealed: Boolean) {
    StatusBadge(
        label = if (sealed) PropertyStrings.sealedLabel else PropertyStrings.draftLabel,
        icon = if (sealed) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
    )
}
