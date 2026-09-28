package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

// Home: address → property → tenancy → inspection → template.
// Direct entry, no server step, no signup, no login.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Don't pay for what you didn't break.")
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Property address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        error?.let { Text("Error: $it") }
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val prop = container.api.createProperty(address.trim())
                        val ten = container.api.createTenancy(prop.id)
                        val ins = container.api.createInspection(ten.id)
                        container.api.ensureTemplate(ins.id)
                        onReady(ins.id)
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && address.trim().length >= 3,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Starting…" else "Protect this tenancy")
        }
    }
}
