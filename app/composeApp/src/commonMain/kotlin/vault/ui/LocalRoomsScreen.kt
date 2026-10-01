package vault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.LocalSession
import vault.local.LocalValidationException
import vault.local.db.LocalRoom

// LocalRoomsScreen: manually named rooms, offline. Add, rename, reorder
// and delete in DRAFT; labels freeze when sealed. Unicode preserved.
// Rooms with evidence cannot be deleted until the evidence is removed.
@Composable
fun LocalRoomsScreen(
    session: LocalSession,
    inspectionId: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<LocalRoom>>(emptyList()) }
    var percents by remember { mutableStateOf(mapOf<String, Int>()) }
    var counts by remember { mutableStateOf(mapOf<String, Int>()) }
    var newLabel by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<LocalRoom?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<LocalRoom?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("entry") }

    fun refresh() {
        scope.launch {
            try {
                kind = session.repository.getInspection(inspectionId)?.kind ?: "entry"
                val list = session.repository.roomsFor(inspectionId)
                rooms = list
                percents = list.associate { it.id to session.coverage.roomCoverage(it.id).percent() }
                counts = list.associate { it.id to session.repository.evidencesForRoom(it.id).size }
                error = null
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.rooms + if (kind == "exit") " — ${Strings.exitKind}" else "")
        ErrorText(error)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VaultTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                label = Strings.roomNameHint,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            session.repository.addRoom(inspectionId, newLabel)
                            newLabel = ""
                            refresh()
                        } catch (e: Exception) {
                            error = e.message?.take(160)
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(Strings.add) }
        }
        if (rooms.isEmpty()) BodyText(Strings.noRoomsYet)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(rooms, key = { it.id }) { room ->
                LocalRoomRow(
                    room = room,
                    percent = percents[room.id] ?: 0,
                    evidenceCount = counts[room.id] ?: 0,
                    canMoveUp = rooms.firstOrNull()?.id != room.id,
                    canMoveDown = rooms.lastOrNull()?.id != room.id,
                    onOpen = { onOpenRoom(room.id, room.label) },
                    onRename = { renaming = room; renameText = room.label },
                    onMove = { delta ->
                        scope.launch {
                            try {
                                val idx = rooms.indexOfFirst { it.id == room.id }
                                session.repository.moveRoom(room.id, idx + delta)
                                refresh()
                            } catch (e: Exception) {
                                error = e.message?.take(160)
                            }
                        }
                    },
                    onDelete = { deleting = room },
                )
            }
        }
        VaultButton(text = Strings.back, onClick = onBack)
    }

    renaming?.let { room ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(Strings.rename) },
            text = {
                VaultTextField(value = renameText, onValueChange = { renameText = it }, label = room.label)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            session.repository.renameRoom(room.id, renameText)
                            renaming = null
                            refresh()
                        } catch (e: Exception) {
                            error = e.message?.take(160)
                        }
                    }
                }) { Text(Strings.save) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(Strings.cancel) } },
        )
    }

    deleting?.let { room ->
        val attached = counts[room.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(Strings.deleteRoomTitle) },
            text = {
                Text(if (attached > 0) Strings.deleteRoomHasEvidence else room.label)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                session.repository.deleteRoom(room.id)
                                deleting = null
                                refresh()
                            } catch (e: LocalValidationException) {
                                error = e.message?.take(200)
                                deleting = null
                            }
                        }
                    },
                    enabled = attached == 0,
                ) { Text(Strings.delete) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(Strings.cancel) } },
        )
    }
}

@Composable
private fun LocalRoomRow(
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
        if (canMoveUp) TextButton(onClick = { onMove(-1) }) { Text("↑") }
        if (canMoveDown) TextButton(onClick = { onMove(1) }) { Text("↓") }
        TextButton(onClick = onRename) { Text(Strings.rename) }
        TextButton(onClick = onDelete) { Text(Strings.delete) }
    }
}
