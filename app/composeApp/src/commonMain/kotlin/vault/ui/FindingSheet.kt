package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import vault.findings.FindingPreset

// FindingSheet: photo + preset chips + optional note. No mandatory text.
@Composable
fun FindingSheet(
    container: AppContainer,
    roomId: String,
    evidenceId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var preset by remember { mutableStateOf(FindingPreset.CRACK) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("What did you find?")
        FindingPreset.entries.forEach { p ->
            FilterChip(
                selected = p == preset,
                onClick = { preset = p },
                label = { Text(p.name.lowercase()) },
            )
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
                        container.api.createFinding(roomId, evidenceId, preset.name.lowercase(), note)
                        onSaved()
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Saving…" else "Save finding")
        }
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
