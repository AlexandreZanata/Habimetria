package vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Home matches the product mockup (no illustrations): hero title, big
// "Nova vistoria" card, "Meus imóveis" cards with nickname, room count,
// status pill, ⋮ menu (new inspection, rename) and Continuar, plus an
// outlined "＋ Novo imóvel" opener. Every color is a theme role, so the
// layout holds in light and dark mode. Remote mode keeps the single
// address form (optional API integration with template rooms).
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
    var renaming by remember { mutableStateOf<PropertyCard?>(null) }
    var renameText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val local = container.local

    suspend fun load() {
        val repo = local?.repository ?: return
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
            if (cards == null && properties.isNotEmpty()) showForm = false
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

    LaunchedEffect(local) { load() }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(Strings.appName, style = MaterialTheme.typography.headlineMedium)
        Text(
            Strings.tagline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (local != null) {
            HeroNewInspection(enabled = !busy, onClick = { showForm = true })
            val propertyCards = cards.orEmpty()
            if (propertyCards.isNotEmpty()) {
                val count = propertyCards.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Strings.myProperties, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$count ${if (count == 1) Strings.propertySingular else Strings.propertyPlural}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                propertyCards.forEach { card ->
                    PropertyCardView(
                        card = card,
                        busy = busy,
                        onResume = { resume(it) },
                        onNewInspection = { startOnProperty(card.id) },
                        onRename = { renaming = card; renameText = "" },
                    )
                }
            }
            OutlinedButton(
                onClick = { showForm = !showForm },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("+ ${Strings.newProperty}") }
        }
        if (local == null || showForm) {
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
        }
        ErrorText(error)
        if (local == null || showForm) {
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

    renaming?.let { card ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(Strings.nicknameTitle) },
            text = {
                VaultTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = card.title,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            local?.repository?.setNickname(card.id, renameText)
                            renaming = null
                            load()
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        }
                    }
                }) { Text(Strings.save) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(Strings.cancel) } },
        )
    }
}

@Composable
private fun HeroNewInspection(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                Strings.newInspection,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "›",
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun PropertyCardView(
    card: PropertyCard,
    busy: Boolean,
    onResume: (inspectionId: String) -> Unit,
    onNewInspection: () -> Unit,
    onRename: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val latest = card.drafts.firstOrNull()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "⌂",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${latest?.rooms ?: 0} ${Strings.roomsSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text(
                            "⋮",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(Strings.newInspection) },
                            onClick = { menu = false; onNewInspection() },
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.renameNickname) },
                            onClick = { menu = false; onRename() },
                        )
                    }
                }
            }
            card.address?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        Strings.newInspection,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (latest != null) {
                VaultButton(
                    text = "${Strings.continueAction} ›",
                    onClick = { onResume(latest.id) },
                    enabled = !busy,
                )
            }
        }
    }
}
