package vault.ui.property

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import vault.local.NavigationProgress
import vault.ui.*
import vault.ui.VaultAlertDialog

@Composable
fun InspectionProfileDialog(container: AppContainer, propertyId: String, onReady: (String) -> Unit, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun start(technical: Boolean) {
        busy = true
        scope.launch {
            try {
                if (technical) {
                    val session = requireNotNull(container.local)
                    val inspection = session.repository.createTechnicalInspection(propertyId)
                    session.navigation.save(NavigationProgress(inspection.id))
                    onReady(inspection.id)
                } else startOnProperty(container, propertyId, onReady)
                onClose()
            } catch (e: Exception) { error = userErrorMessage(e) }
            finally { busy = false }
        }
    }
    VaultAlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Tipo de vistoria") },
        text = { androidx.compose.foundation.layout.Column {
            Text("Locação: registro de entrada e saída. Profissional: vistoria do imóvel com responsável, escopo e ART/RRT declarados.")
            ErrorText(error)
        } },
        confirmButton = { TextButton(onClick = { start(true) }, enabled = !busy) { Text("Profissional") } },
        dismissButton = { TextButton(onClick = { start(false) }, enabled = !busy) { Text("Locação") } })
}
