package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vault.local.PropertyOverview
import vault.ui.Strings
import vault.ui.VaultButton

// Property card driven by the aggregated overview: no per-card queries.
// Reference layout: cover thumbnail left, title + menu, address with pin,
// rooms + status chips, Continuar for the open draft.
@Composable
fun PropertyCardView(
    overview: PropertyOverview,
    busy: Boolean,
    onResume: (inspectionId: String) -> Unit,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val property = overview.property
    val title = property.nickname.ifBlank { property.address }
    val addressLines = if (property.nickname.isNotBlank()) {
        property.address.split(" — ").filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                PropertyCover(photoPath = property.photoPath)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            IconButton(
                                onClick = { menu = true },
                                enabled = !busy,
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription =
                                        "${PropertyStrings.optionsDescription}: $title"
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
                                            Strings.newInspection,
                                            modifier = Modifier.semantics {
                                                contentDescription = "${Strings.newInspection}: $title"
                                            },
                                        )
                                    },
                                    onClick = { menu = false; onNewInspection() },
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
                                    onClick = { menu = false; onOpenDetail() },
                                )
                                if (onRemove != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                PropertyStrings.removeShort,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.semantics {
                                                    contentDescription =
                                                        "${PropertyStrings.removeShort}: $title"
                                                },
                                            )
                                        },
                                        onClick = { menu = false; onRemove() },
                                    )
                                }
                            }
                        }
                    }
                    if (addressLines.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                addressLines.joinToString("\n"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoomsChip(overview.totalRooms)
                        StatusChip(archived = property.archived)
                    }
                }
            }
            val draftId = overview.latestDraftId
            if (draftId != null) {
                VaultButton(
                    text = Strings.continueAction,
                    onClick = { onResume(draftId) },
                    enabled = !busy,
                )
            }
        }
    }
}
