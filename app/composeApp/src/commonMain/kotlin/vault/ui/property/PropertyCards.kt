package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vault.local.PropertyOverview
import vault.ui.Strings
import vault.ui.VaultButton

// Cover spans the metadata row. Title is one line and address reserves two,
// so text length does not change normal card height. Larger fonts can grow.
@Composable
fun PropertyCardView(
    overview: PropertyOverview,
    busy: Boolean,
    onResume: (inspectionId: String) -> Unit,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val property = overview.property
    val title = property.nickname.ifBlank { property.address }
    val addressLines = if (property.nickname.isNotBlank()) {
        property.address.split(" — ").filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PropertyCardRow(photoPath = property.photoPath) {
                Box {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(end = 40.dp),
                        )
                        if (addressLines.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                )
                                Text(
                                    addressLines.joinToString("\n"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            RoomsChip(overview.totalRooms)
                            StatusChip(archived = property.archived)
                        }
                    }
                    PropertyCardMenu(
                        title = title,
                        busy = busy,
                        onNewInspection = onNewInspection,
                        onOpenDetail = onOpenDetail,
                        onRemove = onRemove,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp),
                    )
                }
            }
            overview.latestDraftId?.let { draftId ->
                VaultButton(
                    text = Strings.continueAction,
                    onClick = { onResume(draftId) },
                    enabled = !busy,
                )
            }
        }
    }
}
