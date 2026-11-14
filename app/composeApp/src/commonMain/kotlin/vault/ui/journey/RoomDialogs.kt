package vault.ui.journey

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import vault.local.db.LocalRoom
import vault.ui.Strings
import vault.ui.VaultTextField
import vault.ui.VaultAlertDialog

// Rename dialog for a manually named room. DRAFT-only by caller.
@Composable
fun RoomRenameDialog(
    room: LocalRoom,
    text: String,
    onText: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    VaultAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.rename) },
        text = {
            VaultTextField(value = text, onValueChange = onText, label = room.label)
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(Strings.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
    )
}

// Delete confirmation: rooms with evidence stay blocked until the
// evidence is removed (explicit decision, never silent data loss).
@Composable
fun RoomDeleteDialog(
    roomLabel: String,
    attached: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    VaultAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.deleteRoomTitle) },
        text = {
            Text(if (attached > 0) Strings.deleteRoomHasEvidence else roomLabel)
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = attached == 0) { Text(Strings.delete) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
    )
}
