package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import vault.api.SealResult

// Seal: one tap freezes the vault. Shows the verify code the app
// embeds (as QR) into the final PDF in M4.
@Composable
fun SealScreen(
    container: AppContainer,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<SealResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        result?.let {
            Text("Sealed. Evidence: ${it.evidenceCount}")
            Text("Verify code: ${it.verifyCode}")
            Text("PDF arrives in M4.")
        } ?: run {
            Text("Sealing freezes the originals. Nothing changes afterwards.")
            error?.let { Text("Error: $it") }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            result = container.api.seal(inspectionId)
                        } catch (e: Exception) {
                            error = e.message?.take(200)
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Sealing…" else "Seal now")
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
