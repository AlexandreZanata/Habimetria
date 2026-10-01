package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.ui.Strings

// Contextual shortcuts inside one inspection journey. Secondary chrome:
// the primary task action of each screen stays prominent. A null entry
// hides that shortcut (already there). Header Back + global bar remain
// the only hierarchical navigation.
@Composable
fun JourneyShortcuts(
    onRooms: (() -> Unit)?,
    onReview: (() -> Unit)?,
) {
    if (onRooms == null && onReview == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        onRooms?.let {
            TextButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(Strings.rooms)
            }
        }
        onReview?.let {
            TextButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(Strings.reviewShort)
            }
        }
    }
}
