package vault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.RoomPreset
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.ScreenTitle
import vault.ui.SkeletonLine
import vault.ui.VaultTextField
import vault.ui.userErrorMessage

// Cômodos padrão: preset room names managed once and copied into any
// DRAFT inspection on demand. Whole-list edits here never touch rooms
// already created or frozen in sealed inspections.
@Composable
fun RoomPresetsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val local = container.local
    var presets by remember { mutableStateOf<List<RoomPreset>?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<RoomPreset?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun load() {
        try {
            presets = local?.roomPresets?.list()
        } catch (e: Exception) {
            error = userErrorMessage(e)
        }
    }

    LaunchedEffect(local) { load() }

    fun run(work: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = null
            try {
                work()
                load()
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(SettingsStrings.roomPresetsTitle)
            Text(
                SettingsStrings.roomPresetsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { ErrorText(error) }
        if (presets == null) {
            item { ProgressStatus(true) }
            item { SkeletonLine(0.5f, 20.dp) }
            item { SkeletonLine(0.4f, 20.dp) }
            item { SkeletonLine(0.6f, 20.dp) }
        } else {
            if (presets!!.isEmpty()) {
                item {
                    Text(
                        SettingsStrings.roomPresetsEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(presets!!, key = { it.id }) { preset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            preset.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(
                            onClick = { renaming = preset; renameText = preset.label },
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = SettingsStrings.renameRoomPresetDescription,
                            )
                        }
                        IconButton(
                            onClick = { run { local?.roomPresets?.delete(preset.id) } },
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = SettingsStrings.deleteRoomPresetDescription,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VaultTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = SettingsStrings.newRoomPresetLabel,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { run { local?.roomPresets?.add(draft); draft = "" } },
                    enabled = !busy && draft.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = SettingsStrings.addRoomPreset)
                }
            }
        }
    }

    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(SettingsStrings.renameRoomPresetTitle) },
            text = {
                VaultTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = SettingsStrings.newRoomPresetLabel,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank() && !busy,
                    onClick = {
                        run { local?.roomPresets?.rename(target.id, renameText) }
                        renaming = null
                    },
                ) { Text(SettingsStrings.renameConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text(SettingsStrings.renameCancel) }
            },
        )
    }
}
