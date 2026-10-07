package vault.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val kind = if (overview.inspection.kind == "exit") InspectionStrings.kindExit else InspectionStrings.kindEntry
    val title = PropertyStrings.archivedSuffix(
        overview.propertyLabel.ifBlank { InspectionStrings.unknownProperty },
        overview.propertyArchived,
    )
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
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
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (sealed) Strings.review else Strings.continueAction,
                                    modifier = Modifier.semantics {
                                        contentDescription = if (sealed) {
                                            "${Strings.review}: $title"
                                        } else {
                                            "${Strings.continueAction}: $title"
                                        }
                                    },
                                )
                            },
                            onClick = {
                                menu = false
                                if (sealed) onReview() else onResume()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    PropertyStrings.removeShort,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.semantics {
                                        contentDescription = "${PropertyStrings.removeShort}: $title"
                                    },
                                )
                            },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    kind,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBadge(sealed = sealed)
            }
        }
    }
}

// Status pill: green check for sealed, neutral clock for draft. Icon
// plus text, so state never depends on color alone.
@Composable
private fun StatusBadge(sealed: Boolean) {
    val container = if (sealed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (sealed) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (sealed) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                if (sealed) PropertyStrings.sealedLabel else PropertyStrings.draftLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
