package vault.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    ScreenColumn {
        ScreenTitle(Strings.sealTitle)
        BodyText(Strings.sealWarning)
        result?.let {
            Text(Strings.sealed)
            Text("${Strings.code}: ${it.verifyCode}")
            Text("${Strings.evidence}: ${it.evidenceCount}")
            VaultButton(text = Strings.generatePdf, onClick = { onPdf(it.reportId, it.verifyCode) })
        }
        ErrorText(error)
        if (result == null) {
            VaultButton(
                text = if (busy) Strings.sealing else Strings.sealNow,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            result = container.api.seal(inspectionId)
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
        VaultButton(text = Strings.back, onClick = onBack)
    }
}
