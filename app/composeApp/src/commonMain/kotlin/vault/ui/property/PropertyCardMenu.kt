package vault.ui.property

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import vault.ui.Strings

// Independent menu overlay keeps its 48 dp target without enlarging the title row.
@Composable
internal fun PropertyCardMenu(
    title: String,
    busy: Boolean,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        Strings.newInspection,
                        modifier = Modifier.semantics {
                            contentDescription = "${Strings.newInspection}: $title"
                        },
                    )
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
                onClick = { expanded = false; onOpenDetail() },
            )
            if (onRemove != null) {
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
                    onClick = { expanded = false; onRemove() },
                )
            }
        }
    }
}
