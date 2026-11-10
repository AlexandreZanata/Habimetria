package vault.ui.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

// Home uses a wide cover; management keeps the denser side-by-side row.
@Composable
fun PropertyCardView(
    overview: PropertyOverview,
    busy: Boolean,
    onResume: (inspectionId: String) -> Unit,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)? = null,
    featured: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    val property = overview.property
    val title = property.nickname.ifBlank { property.address }
    val addressLines = if (property.nickname.isNotBlank()) {
        property.address.split(" — ").filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val metadata: @Composable () -> Unit = {
                PropertyCardMetadata(
                    title, addressLines, overview, busy, onNewInspection, onOpenDetail, onRemove,
                )
            }
            if (featured) {
                PropertyCover(property.photoPath, Modifier.fillMaxWidth().height(144.dp))
                metadata()
            } else {
                PropertyCardRow(property.photoPath, metadata)
            }
            overview.latestDraftId?.let { draftId ->
                VaultButton(
                    text = Strings.continueAction,
                    onClick = { onResume(draftId) },
                    enabled = !busy,
                )
            }
            actions?.invoke()
        }
    }
}

@Composable
private fun PropertyCardMetadata(
    title: String,
    addressLines: List<String>,
    overview: PropertyOverview,
    busy: Boolean,
    onNewInspection: () -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PropertyCardMenu(
                title = title,
                busy = busy,
                onNewInspection = onNewInspection,
                onOpenDetail = onOpenDetail,
                onRemove = onRemove,
            )
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
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                )
                Text(
                    addressLines.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FlowRow(
            modifier = Modifier.padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RoomsChip(overview.totalRooms)
            val status = when {
                overview.property.archived -> PropertyStrings.archivedBadge
                overview.latestDraftId != null -> PropertyStrings.filterDraft
                else -> Strings.activeLabel
            }
            StatusChip(status)
        }
    }
}
