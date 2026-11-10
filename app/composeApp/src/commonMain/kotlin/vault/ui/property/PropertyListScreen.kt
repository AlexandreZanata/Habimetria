package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.PropertyOverview
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.LoadingSkeleton
import vault.ui.PropertyCardSkeleton
import vault.ui.RecordPanel
import vault.ui.Strings
import vault.ui.rememberLoadingVisible
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
    var cards by remember { mutableStateOf<List<PropertyOverview>?>(null) }
    var removing by remember { mutableStateOf<PropertyOverview?>(null) }
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

    val showLoading = rememberLoadingVisible(cards == null)

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
            RecordPanel(Icons.Outlined.Apartment, PropertyStrings.listTitle, PropertyStrings.listBody)
        }
        item {
            PropertySearchField(query = query, onQueryChange = { query = it })
        }
        item { VisibilityFilterRow(selected = visibility, onSelect = { visibility = it }) }
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
        if (error != null && !showForm) {
            item { ErrorText(error) }
        }
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
            showLoading -> {
                item(key = "loading") {
                    LoadingSkeleton { repeat(3) { PropertyCardSkeleton() } }
                }
            }
            cards == null -> {
                // Grace period: data is on its way, paint nothing yet.
            }
            filtered.isEmpty() -> {
                item { PropertyListEmptyState(query = query, visibility = visibility) }
            }
            else -> {
                items(filtered, key = { it.property.id }) { overview ->
                    PropertyListCard(
                        container = container,
                        overview = overview,
                        scope = scope,
                        busy = busy,
                        onBusy = { busy = it },
                        onError = { error = it },
                        onReady = onReady,
                        onOpenDetail = { onOpenDetail(overview.property.id) },
                        onRemove = { removing = overview },
                        onRestored = {
                            load()
                            feedback = PropertyStrings.restoredOk
                        },
                    )
                }
            }
        }
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
