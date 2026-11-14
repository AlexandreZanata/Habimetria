package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.local.db.LocalProperty
import vault.ui.AppContainer

@Composable
internal fun PropertyDetailHeader(container: AppContainer, property: LocalProperty, onCoverSaved: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var coverRevision by remember(property.id) { mutableIntStateOf(0) }
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            key(coverRevision) {
                PropertyCover(property.photoPath, Modifier.fillMaxWidth().height(PropertyFeaturedCoverHeight))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    property.nickname.ifBlank { property.address },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.primary,
                    modifier = Modifier.semantics { heading() },
                )
                if (property.nickname.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Place, null, Modifier.padding(top = 2.dp).size(16.dp), tint = colors.onSurfaceVariant)
                        Text(property.address, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (property.archived) ArchivedBadge()
            }
            HorizontalDivider(color = colors.outlineVariant)
            PropertyCoverActions(container, property.id, property.photoPath) {
                coverRevision++
                onCoverSaved()
            }
        }
    }
}
