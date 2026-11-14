package vault.ui.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.ui.Strings

// The menu remains a separate 48 dp target from the Property title.
@Composable
internal fun PropertyCardMenu(
    title: String,
    busy: Boolean,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = !busy,
            modifier = Modifier.semantics {
                contentDescription = "${PropertyStrings.optionsDescription}: $title"
            },
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp),
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        Strings.newInspection,
                        modifier = Modifier.semantics {
                            contentDescription = "${Strings.newInspection}: $title"
                        },
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, tint = colors.primary)
                },
                onClick = { expanded = false; onNewInspection() },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        PropertyStrings.propertyData,
                        modifier = Modifier.semantics {
                            contentDescription = "${PropertyStrings.propertyData}: $title"
                        },
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary)
                },
                onClick = { expanded = false; onOpenDetail() },
            )
            if (onRemove != null) {
                HorizontalDivider(color = colors.outlineVariant)
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
                    colors = MenuDefaults.itemColors(textColor = colors.error, leadingIconColor = colors.error),
                    onClick = { expanded = false; onRemove() },
                )
            }
        }
    }
}
