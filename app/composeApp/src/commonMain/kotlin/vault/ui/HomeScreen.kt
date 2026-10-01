package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import vault.local.PropertyOverview
import vault.ui.property.HeroNewInspection
import vault.ui.property.HomePropertyStates
import vault.ui.property.PropertyAddressForm
import vault.ui.property.PropertyRenameDialog
import vault.ui.property.createPropertyAndStart
import vault.ui.property.isPropertyFormValid
import vault.ui.property.startOnProperty

// Home prioritizes new/resume: hero entry, Continuar for the latest
// draft and the property creation form. Full management lives in
// Imóveis; this screen keeps cards read-mostly until then.
// Loading, error and empty are distinct states with retry.
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
    var loadError by remember { mutableStateOf<String?>(null) }
    var cards by remember { mutableStateOf<List<PropertyOverview>?>(null) }
    var showForm by remember { mutableStateOf(true) }
    var renaming by remember { mutableStateOf<PropertyOverview?>(null) }
    var renameText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val local = container.local

    suspend fun load() {
        val repo = local?.repository ?: return
        try {
            val rows = repo.propertyOverviews(limit = 100)
            if (cards == null && rows.isNotEmpty()) showForm = false
            rows.firstOrNull()?.property?.let {
                if (street.isBlank() && it.street.isNotBlank()) street = it.street
                if (number.isBlank() && it.number.isNotBlank()) number = it.number
                if (complement.isBlank()) complement = it.complement.orEmpty()
                if (neighborhood.isBlank() && it.neighborhood.isNotBlank()) neighborhood = it.neighborhood
            }
            cards = rows
            loadError = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
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

    fun startProperty(propertyId: String) {
        scope.launch {
            busy = true
            error = null
            try {
                startOnProperty(container, propertyId, onReady)
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
            HomePropertyStates(
                rows = cards,
                loadError = loadError,
                busy = busy,
                onRetry = { scope.launch { load() } },
                onResume = ::resume,
                onNewInspection = ::startProperty,
                onRename = { renaming = it; renameText = "" },
            )
            OutlinedButton(
                onClick = { showForm = !showForm },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Strings.newProperty) }
        }
        if (local == null || showForm) {
            PropertyAddressForm(
                nickname, { nickname = it }, street, { street = it },
                number, { number = it }, complement, { complement = it },
                neighborhood, { neighborhood = it }, showNickname = local != null,
            )
        }
        ErrorText(error)
        if (local == null || showForm) {
            VaultButton(
                text = if (busy) Strings.starting else Strings.start,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            createPropertyAndStart(
                                container, nickname, street, number, complement, neighborhood, onReady,
                            )
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && isPropertyFormValid(street, number, neighborhood),
            )
        }
    }

    renaming?.let { overview ->
        PropertyRenameDialog(
            currentTitle = overview.property.nickname.ifBlank { overview.property.address },
            draft = renameText,
            onDraft = { renameText = it },
            onConfirm = {
                scope.launch {
                    try {
                        local?.repository?.setNickname(overview.property.id, renameText)
                        renaming = null
                        load()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
            onDismiss = { renaming = null },
        )
    }
}


