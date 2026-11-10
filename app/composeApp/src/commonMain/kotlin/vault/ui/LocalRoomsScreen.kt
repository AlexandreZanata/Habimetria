package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.LocalSession
import vault.local.LocalValidationException
import vault.local.STATUS_DRAFT
import vault.local.db.LocalRoom
import vault.ui.journey.JourneyPrimaryAction
import vault.ui.journey.JourneyStrings
import vault.ui.journey.RoomDeleteDialog
import vault.ui.journey.RoomEmptyState
import vault.ui.journey.RoomListRow
import vault.ui.journey.RoomRenameDialog
import vault.ui.journey.RoomSetupForm

@Composable
fun LocalRoomsScreen(
    session: LocalSession,
    inspectionId: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onStartCapture: (roomId: String, label: String) -> Unit,
    onSealed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<LocalRoom>?>(null) }
    var percents by remember { mutableStateOf(mapOf<String, Int>()) }
    var counts by remember { mutableStateOf(mapOf<String, Int>()) }
    var newLabel by rememberSaveable(inspectionId) { mutableStateOf("") }
    var renaming by remember { mutableStateOf<LocalRoom?>(null) }
    var renameText by rememberSaveable(inspectionId) { mutableStateOf("") }
    var deleting by remember { mutableStateOf<LocalRoom?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("entry") }
    var sealedRedirected by remember { mutableStateOf(false) }
    var presetCount by remember { mutableStateOf(0) }
    var addingRooms by remember { mutableStateOf(false) }

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
                presetCount = session.roomPresets.list().size
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    fun addRooms(write: suspend () -> Unit) {
        if (addingRooms || rooms == null) return
        addingRooms = true
        error = null
        scope.launch {
            try {
                write()
                refresh()
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                addingRooms = false
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    val showLoading = rememberLoadingVisible(rooms == null)

    ScreenColumn {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                RoomSetupForm(
                    isExit = kind == "exit",
                    error = error,
                    value = newLabel,
                    onValue = { newLabel = it },
                    canAdd = rooms != null && !addingRooms,
                    onAdd = {
                        val label = newLabel
                        addRooms {
                            session.repository.addRoom(inspectionId, label)
                            if (newLabel == label) newLabel = ""
                        }
                    },
                    presetCount = presetCount,
                    onAddPresets = { addRooms { session.repository.addRoomsFromPresets(inspectionId) } },
                )
            }
            val list = rooms.orEmpty()
            if (showLoading) {
                item { ProgressStatus(true) }
                item { RoomRowSkeleton() }
                item { RoomRowSkeleton() }
                item { RoomRowSkeleton() }
            } else if (rooms != null && list.isEmpty()) {
                item { RoomEmptyState() }
            } else if (list.isNotEmpty()) {
                item { Text(JourneyStrings.roomCount(list.size), style = MaterialTheme.typography.titleSmall) }
            }
            items(list, key = { it.id }) { room ->
                RoomListRow(
                    room = room,
                    percent = percents[room.id] ?: 0,
                    evidenceCount = counts[room.id] ?: 0,
                    canMoveUp = list.firstOrNull()?.id != room.id,
                    canMoveDown = list.lastOrNull()?.id != room.id,
                    onOpen = { onOpenRoom(room.id, room.label) },
                    onRename = { renaming = room; renameText = room.label },
                    onMove = { delta ->
                        scope.launch {
                            try {
                                val idx = list.indexOfFirst { it.id == room.id }
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
            list.firstOrNull()?.let { first ->
                item {
                    JourneyPrimaryAction(
                        text = Strings.startInspection,
                        icon = Icons.Filled.CameraAlt,
                        onClick = { onStartCapture(first.id, first.label) },
                        enabled = !addingRooms,
                    )
                }
            }
        }
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
