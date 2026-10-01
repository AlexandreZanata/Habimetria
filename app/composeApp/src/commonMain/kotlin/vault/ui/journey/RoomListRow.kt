package vault.ui.journey

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.db.LocalRoom
import vault.ui.Strings

// One room row: open, rename, reorder and delete entries.
// Coverage percent and evidence count stay visible per room.
@Composable
fun RoomListRow(
    room: LocalRoom,
    percent: Int,
    evidenceCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${room.label} · $percent% · $evidenceCount ${Strings.photosSuffix}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (canMoveUp) {
            IconButton(onClick = { onMove(-1) }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Strings.moveUp)
            }
        }
        if (canMoveDown) {
            IconButton(onClick = { onMove(1) }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = Strings.moveDown)
            }
        }
        TextButton(onClick = onRename) { Text(Strings.rename) }
        TextButton(onClick = onDelete) { Text(Strings.delete) }
    }
}
