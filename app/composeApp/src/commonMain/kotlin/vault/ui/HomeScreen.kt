package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.NavigationProgress

private data class DraftResume(
    val id: String,
    val address: String,
    val rooms: Int,
    val kind: String,
)

// Home: address → property → tenancy → inspection.
// Local session present: full offline journey, no templates.
// Unfinished DRAFT inspections are listed first: going back never
// loses rooms, the same inspection resumes with its data intact.
// Otherwise the optional API integration with template rooms.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<DraftResume>?>(null) }
    val scope = rememberCoroutineScope()
    val local = container.local

    LaunchedEffect(local) {
        val repo = local?.repository ?: return@LaunchedEffect
        drafts = try {
            repo.listDrafts().map { inspection ->
                DraftResume(
                    id = inspection.id,
                    address = repo.addressFor(inspection.id) ?: "—",
                    rooms = repo.roomsFor(inspection.id).size,
                    kind = inspection.kind,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun resume(id: String) {
        scope.launch {
            try {
                local?.navigation?.save(NavigationProgress(id))
                onReady(id)
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    ScreenColumn {
        ScreenTitle(Strings.appName)
        Text(Strings.tagline)
        val pending = drafts
        if (local != null && pending != null && pending.isNotEmpty()) {
            Text(Strings.resumeTitle, style = MaterialTheme.typography.titleMedium)
            pending.forEach { draft ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draft.address +
                            (if (draft.kind == "exit") " — ${Strings.exitKind}" else "") +
                            " · ${draft.rooms} ${Strings.roomsSuffix}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { resume(draft.id) }) { Text(Strings.continueAction) }
                }
            }
        }
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
