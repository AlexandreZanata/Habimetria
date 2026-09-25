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

// Seal: one tap, server ack with verify code. No login, no paywall
// in MVP (PAYMENTS_MODE=off). PDF arrives in M4.
@Composable
fun SealScreen(
    container: AppContainer,
    inspectionId: String,
    onPdf: (reportId: String, verifyCode: String) -> Unit,
    onBack: () -> Unit,
) {
    var result by remember { mutableStateOf<SealResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Seal this inspection")
        Text("Sealed photos can't be replaced. New problems become occurrences.")
        result?.let {
            Text("Sealed ✓")
            Text("Code: ${it.verifyCode}")
            Text("Evidence: ${it.evidenceCount}")
            Button(
                onClick = { onPdf(it.reportId, it.verifyCode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate PDF")
            }
        }
        error?.let { Text("Error: $it") }
        if (result == null) {
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            result = container.api.seal(inspectionId)
                        } catch (e: Exception) {
                            error = e.message?.take(200)
                        } finally {
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
