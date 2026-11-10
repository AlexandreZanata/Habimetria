package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// FindingSheet: photo + preset chips + optional note. No mandatory text.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingSheet(
    container: AppContainer,
    roomId: String,
    evidenceId: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf("stain", "scratch", "crack", "broken", "moisture", "hole", "other")
    var preset by remember { mutableStateOf("stain") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    VaultModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(Strings.whatFound)
            presets.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { preset = p },
                            label = { Text(p) },
                        )
                    }
                }
            }
            VaultTextField(
                value = note,
                onValueChange = { note = it },
                label = Strings.noteOptional,
                singleLine = false,
            )
            ErrorText(error)
            VaultButton(
                text = if (busy) Strings.savingFinding else Strings.saveFinding,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            container.api.createFinding(roomId, evidenceId, preset, note.ifBlank { null })
                            onDone()
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            )
        }
    }
}
