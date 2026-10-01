package vault.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import vault.local.InspectionOverview
import vault.local.STATUS_SEALED
import vault.ui.Strings
import vault.ui.property.PropertyStrings

// Inspection row: title plus an options menu, in DRAFT and SEALED
// alike. Primary actions (continue/review) and destructive removal
// both live behind the menu; nothing executes without an explicit tap.
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
    val title = "$kind · ${if (sealed) PropertyStrings.sealedLabel else PropertyStrings.draftLabel}"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
}
