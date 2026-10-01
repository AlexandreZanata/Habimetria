package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import vault.local.STATUS_DRAFT
import vault.local.db.LocalRoom
import vault.ui.journey.JourneyShortcuts
import vault.ui.journey.RoomDeleteDialog
import vault.ui.journey.RoomListRow
import vault.ui.journey.RoomRenameDialog

// LocalRoomsScreen: manually named rooms, offline. Add, rename, reorder
// and delete in DRAFT; labels freeze when sealed. Unicode preserved.
// Rooms with evidence cannot be deleted until the evidence is removed.
@Composable
fun LocalRoomsScreen(
    session: LocalSession,
    inspectionId: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onStartCapture: (roomId: String, label: String) -> Unit,
    onSealed: () -> Unit,
    onReviewShortcut: () -> Unit,
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
    var sealedRedirected by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try {
                val inspection = session.repository.getInspection(inspectionId)
                if (inspection != null && inspection.status != STATUS_DRAFT && !sealedRedirected) {
                    sealedRedirected = true
                    onSealed()
                    return@launch
                }
                kind = inspection?.kind ?: "entry"
                val list = session.repository.roomsFor(inspectionId)
                rooms = list
                percents = list.associate { it.id to session.coverage.roomCoverage(it.id).percent() }
                counts = list.associate { it.id to session.repository.evidencesForRoom(it.id).size }
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.rooms + if (kind == "exit") " — ${Strings.exitKind}" else "")
        JourneyShortcuts(onRooms = null, onReview = onReviewShortcut)
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
                            error = userErrorMessage(e)
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(Strings.add) }
        }
        if (rooms.isEmpty()) BodyText(Strings.noRoomsYet)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(rooms, key = { it.id }) { room ->
                RoomListRow(
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
                                error = userErrorMessage(e)
                            }
                        }
                    },
                    onDelete = { deleting = room },
                )
            }
        }
        VaultButton(
            text = Strings.startInspection,
            onClick = {
                val first = rooms.firstOrNull() ?: return@VaultButton
                onStartCapture(first.id, first.label)
            },
            enabled = rooms.isNotEmpty(),
        )
    }

    renaming?.let { room ->
        RoomRenameDialog(
            room = room,
            text = renameText,
            onText = { renameText = it },
            onConfirm = {
                scope.launch {
                    try {
                        session.repository.renameRoom(room.id, renameText)
                        renaming = null
                        refresh()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { room ->
        RoomDeleteDialog(
            roomLabel = room.label,
            attached = counts[room.id] ?: 0,
            onConfirm = {
                scope.launch {
                    try {
                        session.repository.deleteRoom(room.id)
                        deleting = null
                        refresh()
                    } catch (e: LocalValidationException) {
                        error = userErrorMessage(e)
                        deleting = null
                    }
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

