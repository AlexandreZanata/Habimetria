package vault.ui.journey

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import vault.ui.Strings
import vault.ui.VaultTextField

// First rooms-stage header: explanatory card, labeled name input with
// an inline pale-green add action, and a text-only empty state (no
// decorative artwork). Behavior unchanged: add/rename/reorder/delete
// keep working below this header.

// Pale-green guidance card: what this stage is for.
@Composable
fun RoomInfoCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Strings.reviewShort,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    Strings.roomsReviewBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Labeled input with the add action beside it (never below the fold).
@Composable
fun RoomNameInput(
    value: String,
    onValue: (String) -> Unit,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.roomNameLabel, style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VaultTextField(
                value = value,
                onValueChange = onValue,
                label = Strings.roomNameLabel,
                placeholder = Strings.roomNamePlaceholder,
                leadingIcon = Icons.Outlined.Home,
                modifier = Modifier.weight(1f),
            )
            // Square "+" matching the single-line input height (56dp);
            // screen readers still hear "Adicionar" via description.
            Button(
                onClick = onAdd,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp),
            ) { Icon(Icons.Filled.Add, contentDescription = Strings.add) }
        }
    }
}

// Text-only empty state: title plus next-step hint, no artwork.
@Composable
fun RoomEmptyState() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(Strings.roomsEmptyTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            Strings.roomsEmptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
