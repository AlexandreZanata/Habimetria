package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("What did you find?")
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
            TextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text("Error: $it") }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            container.api.createFinding(roomId, evidenceId, preset, note.ifBlank { null })
                            onDone()
                        } catch (e: Exception) {
                            error = e.message?.take(160)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Saving…" else "Save finding")
            }
        }
    }
}
