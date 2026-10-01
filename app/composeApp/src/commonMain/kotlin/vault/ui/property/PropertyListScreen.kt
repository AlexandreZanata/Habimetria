package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.NavigationProgress
import vault.local.PropertyOverview
import vault.local.db.PropertyRestoreOutcome
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.Strings
import vault.ui.VaultTextField
import vault.ui.userErrorMessage

// Imóveis owns management/history; Home stays resume-focused.
// Distinct loading/error/empty with retry; saveable filter + list state.
@Composable
fun PropertyListScreen(
    container: AppContainer,
    onOpenDetail: (propertyId: String) -> Unit,
    onReady: (inspectionId: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var street by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var complement by rememberSaveable { mutableStateOf("") }
    var neighborhood by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loadError by rememberSaveable { mutableStateOf<String?>(null) }
    var cards by mutableStateOf<List<PropertyOverview>?>(null)
    var renaming by mutableStateOf<PropertyOverview?>(null)
    var renameText by rememberSaveable { mutableStateOf("") }
    var removing by mutableStateOf<PropertyOverview?>(null)
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var visibility by rememberSaveable { mutableStateOf(VISIBILITY_ACTIVE) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val local = container.local

    suspend fun load() {
        val repo = local?.repository ?: return
        try {
            cards = repo.propertyOverviews(limit = 100)
            loadError = null
            feedback = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local) { load() }

    val filtered = (cards.orEmpty()).filter { overview ->
        val matchesVisibility = if (visibility == VISIBILITY_ARCHIVED) {
            overview.property.archived
        } else {
            !overview.property.archived
        }
        if (!matchesVisibility) return@filter false
        val q = query.trim().lowercase()
        if (q.isEmpty()) true else {
            overview.property.nickname.lowercase().contains(q) ||
                overview.property.address.lowercase().contains(q)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            VaultTextField(
                value = query,
                onValueChange = { query = it },
                label = PropertyStrings.search,
            )
        }
        item { VisibilityFilterRow(selected = visibility, onSelect = { visibility = it }) }
        val done = feedback
        if (done != null) {
            item { RemovalFeedback(done) }
        }
        when {
            loadError != null -> {
                item {
                    ErrorText(loadError)
                    OutlinedButton(onClick = { scope.launch { load() } }) { Text(Strings.retry) }
                }
            }
            cards == null -> {
                item {
                    ProgressStatus(true)
                    Text(Strings.loadingProperties)
                }
            }
            filtered.isEmpty() -> {
                item {
                    Text(
                        if (query.isNotBlank()) {
                            PropertyStrings.noMatch
                        } else if (visibility == VISIBILITY_ARCHIVED) {
                            PropertyStrings.noArchived
                        } else {
                            Strings.propertiesEmpty
                        },
                    )
                }
            }
            else -> {
                items(filtered, key = { it.property.id }) { overview ->
                    PropertyCardView(
                        overview = overview,
                        busy = busy,
                        onResume = {
                            scope.launch {
                                try {
                                    local?.navigation?.save(NavigationProgress(it))
                                    onReady(it)
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                }
                            }
                        },
                        onNewInspection = {
                            scope.launch {
                                busy = true
                                try {
                                    startOnProperty(container, overview.property.id, onReady)
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        onRename = { renaming = overview; renameText = "" },
                        onRemove = { removing = overview },
                    )
                    if (overview.property.archived) {
                        RestoreButton(busy = busy) {
                            scope.launch {
                                busy = true
                                try {
                                    when (
                                        local?.propertyRemoval?.restore(overview.property.id)
                                    ) {
                                        PropertyRestoreOutcome.Restored,
                                        PropertyRestoreOutcome.AlreadyActive,
                                        -> {
                                            load()
                                            feedback = PropertyStrings.restoredOk
                                        }
                                        PropertyRestoreOutcome.Missing, null -> {
                                            error = Strings.recordMissing
                                        }
                                    }
                                } catch (e: Exception) {
                                    error = userErrorMessage(e)
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { onOpenDetail(overview.property.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(PropertyStrings.openDetail) }
                }
            }
        }
        propertyCreateItems(
            showForm = showForm,
            onToggle = { showForm = !showForm },
            nickname = nickname,
            onNickname = { nickname = it },
            street = street,
            onStreet = { street = it },
            number = number,
            onNumber = { number = it },
            complement = complement,
            onComplement = { complement = it },
            neighborhood = neighborhood,
            onNeighborhood = { neighborhood = it },
            showNickname = local != null,
            error = error,
            busy = busy,
            onSubmit = {
                busy = true
                error = null
                scope.launch {
                    try {
                        createPropertyAndStart(
                            container, nickname, street, number, complement, neighborhood, onReady,
                        )
                        load()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    } finally {
                        busy = false
                    }
                }
            },
        )
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

    removing?.let { target ->
        PropertyRemovalHost(
            container = container,
            overview = target,
            onCommitted = { message ->
                scope.launch {
                    load()
                    feedback = if (
                        message == PropertyStrings.archivedOk &&
                        visibility == VISIBILITY_ACTIVE
                    ) {
                        "$message ${PropertyStrings.archivedHint}"
                    } else {
                        message
                    }
                    removing = null
                }
            },
            onDismiss = { removing = null },
        )
    }
}
