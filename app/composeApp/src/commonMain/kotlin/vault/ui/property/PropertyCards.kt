package vault.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.PropertyOverview
import vault.ui.Strings
import vault.ui.VaultButton

// Hero entry for a new inspection. One prominent task action per screen.
// Single announcement (glyphs hidden): screen readers hear one button.
@Composable
fun HeroNewInspection(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = Strings.newInspection
            role = Role.Button
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                Strings.newInspection,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

// Property card driven by the aggregated overview: no per-card queries.
// Shows nickname/address, room total, draft/sealed counts and Continuar.
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decorative icon: hidden from assistive tech, the title
                // next to it carries the meaning.
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${overview.totalRooms} ${Strings.roomsSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (property.archived) ArchivedBadge()
                }
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
            if (property.nickname.isNotBlank()) {
                Text(
                    property.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val draftId = overview.latestDraftId
            if (draftId != null) {
                // No chevron glyph: decorative marks get read aloud by
                // screen readers, the button role already implies action.
                VaultButton(
                    text = Strings.continueAction,
                    onClick = { onResume(draftId) },
                    enabled = !busy,
                )
            }
        }
    }
}
