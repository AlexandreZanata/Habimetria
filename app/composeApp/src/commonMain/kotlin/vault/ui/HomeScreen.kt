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
import vault.local.AddressParts
import vault.local.NavigationProgress
import vault.local.formatAddress

private data class DraftResume(
    val id: String,
    val address: String,
    val rooms: Int,
    val kind: String,
)

// Home: checkout-style address → property → tenancy → inspection.
// Local session present: full offline journey, no templates.
// Unfinished DRAFT inspections are listed first and the form prefills
// from the latest one: going back never loses rooms or address data.
// Otherwise the optional API integration with template rooms.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<DraftResume>?>(null) }
    val scope = rememberCoroutineScope()
    val local = container.local

    LaunchedEffect(local) {
        val repo = local?.repository ?: return@LaunchedEffect
        drafts = try {
            val list = repo.listDrafts()
            // Prefill the form from the latest draft so data entered
            // before going to the rooms screen survives coming back.
            if (street.isBlank() && number.isBlank() && neighborhood.isBlank()) {
                list.firstOrNull()?.let { repo.addressPartsFor(it.id) }?.let { parts: AddressParts ->
                    street = parts.street
                    number = parts.number
                    complement = parts.complement
                    neighborhood = parts.neighborhood
                }
            }
            list.map { inspection ->
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
                error = userErrorMessage(e)
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
            value = street,
            onValueChange = { street = it },
            label = Strings.streetLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VaultTextField(
                value = number,
                onValueChange = { number = it },
                label = Strings.numberLabel,
                modifier = Modifier.weight(1f),
            )
            VaultTextField(
                value = complement,
                onValueChange = { complement = it },
                label = Strings.complementLabel,
                modifier = Modifier.weight(2f),
            )
        }
        VaultTextField(
            value = neighborhood,
            onValueChange = { neighborhood = it },
            label = Strings.neighborhoodLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(error)
        val formValid = street.trim().length >= 3 &&
            number.trim().isNotEmpty() &&
            neighborhood.trim().length >= 2
        VaultButton(
            text = if (busy) Strings.starting else Strings.start,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val local = container.local
                        if (local != null) {
                            val prop = local.repository.createPropertyStructured(
                                street = street,
                                number = number,
                                complement = complement.ifBlank { null },
                                neighborhood = neighborhood,
                            )
                            val ten = local.repository.createTenancy(prop.id)
                            val ins = local.repository.createInspection(ten.id)
                            local.navigation.save(NavigationProgress(ins.id))
                            onReady(ins.id)
                        } else {
                            val line = formatAddress(
                                street.trim(), number.trim(),
                                complement.trim(), neighborhood.trim(),
                            )
                            val prop = container.api.createProperty(line)
                            val ten = container.api.createTenancy(prop.id)
                            val ins = container.api.createInspection(ten.id)
                            container.api.ensureTemplate(ins.id)
                            onReady(ins.id)
                        }
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && formValid,
        )
    }
}
