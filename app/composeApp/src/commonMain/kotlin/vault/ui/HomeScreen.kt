package vault.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import vault.local.NavigationProgress

// Home: address → property → tenancy → inspection.
// Local session present: full offline journey, no templates.
// Otherwise the optional API integration with template rooms.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenColumn {
        ScreenTitle(Strings.appName)
        Text(Strings.tagline)
        VaultTextField(
            value = address,
            onValueChange = { address = it },
            label = Strings.addressLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(error)
        VaultButton(
            text = if (busy) Strings.starting else Strings.start,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val local = container.local
                        if (local != null) {
                            val prop = local.repository.createProperty(address.trim())
                            val ten = local.repository.createTenancy(prop.id)
                            val ins = local.repository.createInspection(ten.id)
                            local.navigation.save(NavigationProgress(ins.id))
                            onReady(ins.id)
                        } else {
                            val prop = container.api.createProperty(address.trim())
                            val ten = container.api.createTenancy(prop.id)
                            val ins = container.api.createInspection(ten.id)
                            container.api.ensureTemplate(ins.id)
                            onReady(ins.id)
                        }
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && address.trim().length >= 3,
        )
    }
}
