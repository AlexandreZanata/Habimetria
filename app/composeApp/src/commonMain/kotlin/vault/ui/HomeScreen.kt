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

private data class PropertyCard(
    val id: String,
    val title: String,
    val address: String?,
    val drafts: List<DraftResume>,
)

// Home: properties with nicknames → inspections. Local session present:
// full offline journey, no templates. Each property card lists its
// unfinished DRAFT inspections (going back never loses rooms or address
// data) plus a new-inspection shortcut; "＋ Novo imóvel" opens the
// checkout-style address form. Otherwise the optional API integration
// with template rooms.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cards by remember { mutableStateOf<List<PropertyCard>?>(null) }
    var showForm by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val local = container.local

    LaunchedEffect(local) {
        val repo = local?.repository ?: return@LaunchedEffect
        cards = try {
            val properties = repo.listProperties()
            val drafts = repo.listDrafts().map { inspection ->
                val propertyId = repo.propertyFor(inspection.id)?.id
                propertyId to DraftResume(
                    id = inspection.id,
                    address = repo.addressFor(inspection.id) ?: "—",
                    rooms = repo.roomsFor(inspection.id).size,
                    kind = inspection.kind,
                )
            }.groupBy({ it.first }, { it.second })
            if (properties.isNotEmpty()) showForm = false
            // Prefill the new-property form from the latest draft so data
            // entered before going to the rooms screen survives coming back.
            if (street.isBlank() && number.isBlank() && neighborhood.isBlank()) {
                repo.listDrafts().firstOrNull()?.let { repo.addressPartsFor(it.id) }
                    ?.let { parts: AddressParts ->
                        street = parts.street
                        number = parts.number
                        complement = parts.complement
                        neighborhood = parts.neighborhood
                    }
            }
            properties.map { property ->
                PropertyCard(
                    id = property.id,
                    title = property.nickname.ifBlank { property.address },
                    address = property.address.takeIf { property.nickname.isNotBlank() },
                    drafts = drafts[property.id].orEmpty(),
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

    fun startOnProperty(propertyId: String) {
        scope.launch {
            busy = true
            error = null
            try {
                val repo = local?.repository ?: return@launch
                val ten = repo.createTenancy(propertyId)
                val ins = repo.createInspection(ten.id)
                local.navigation.save(NavigationProgress(ins.id))
                onReady(ins.id)
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    ScreenColumn {
        ScreenTitle(Strings.appName)
        Text(Strings.tagline)
        val propertyCards = cards
        if (local != null && propertyCards != null && propertyCards.isNotEmpty()) {
            Text(Strings.myProperties, style = MaterialTheme.typography.titleMedium)
            propertyCards.forEach { card ->
                Text(card.title, style = MaterialTheme.typography.titleSmall)
                card.address?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                card.drafts.forEach { draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = (if (draft.kind == "exit") "${Strings.exitKind} · " else "") +
                                "${draft.rooms} ${Strings.roomsSuffix}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { resume(draft.id) }) { Text(Strings.continueAction) }
                    }
                }
                TextButton(
                    onClick = { startOnProperty(card.id) },
                    enabled = !busy,
                ) { Text(Strings.newInspection) }
            }
            TextButton(onClick = { showForm = !showForm }) { Text(Strings.newProperty) }
        }
        if (local == null || showForm || propertyCards?.isEmpty() == true) {
        if (local != null) {
            VaultTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = Strings.nicknameLabel,
                modifier = Modifier.fillMaxWidth(),
            )
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
                                nickname = nickname,
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
}
