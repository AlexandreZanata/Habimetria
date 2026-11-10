package vault.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
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
import vault.local.InspectionOverview
import vault.local.NavigationProgress
import vault.local.STATUS_DRAFT
import vault.local.STATUS_SEALED
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.InspectionRowSkeleton
import vault.ui.ProgressStatus
import vault.ui.RecordPanel
import vault.ui.Strings
import vault.ui.rememberLoadingVisible
import vault.ui.VaultTextField
import vault.ui.property.PropertyStrings
import vault.ui.property.RemovalFeedback
import vault.ui.userErrorMessage

// Vistorias grouped by property with Em andamento/Selada filters.
// DRAFT resumes capture/review; SEALED opens read-only review and
// never routes back into capture. Loading/error/empty differ with
// retry; filter and scroll restore across rotation.
@Composable
fun InspectionListScreen(
    container: AppContainer,
    onResume: (inspectionId: String) -> Unit,
    onReview: (inspectionId: String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<InspectionOverview>?>(null) }
    var loadError by rememberSaveable { mutableStateOf<String?>(null) }
    var actionError by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<InspectionOverview?>(null) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val local = container.local

    suspend fun load() {
        val overviews = local?.overviews ?: return
        try {
            val status = when (filter) {
                "DRAFT" -> STATUS_DRAFT
                "SEALED" -> STATUS_SEALED
                else -> null
            }
            rows = overviews.inspectionOverviews(status = status, limit = 100)
            loadError = null
            feedback = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local, filter) { load() }

    val showLoading = rememberLoadingVisible(rows == null)

    val visible = (rows.orEmpty()).filter {
        val q = query.trim().lowercase()
        q.isEmpty() || it.propertyLabel.lowercase().contains(q)
    }

    fun resume(id: String) {
        scope.launch {
            try {
                local?.navigation?.save(NavigationProgress(id))
                onResume(id)
            } catch (e: Exception) {
                actionError = userErrorMessage(e)
            }
        }
    }

    fun review(id: String) {
        scope.launch {
            try {
                local?.navigation?.save(NavigationProgress(id))
                onReview(id)
            } catch (e: Exception) {
                actionError = userErrorMessage(e)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { RecordPanel(Icons.AutoMirrored.Outlined.Assignment, InspectionStrings.listTitle, InspectionStrings.listBody) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { filter = "ALL" },
                    label = { Text(PropertyStrings.filterAll) },
                )
                FilterChip(
                    selected = filter == "DRAFT",
                    onClick = { filter = "DRAFT" },
                    label = { Text(PropertyStrings.filterDraft) },
                )
                FilterChip(
                    selected = filter == "SEALED",
                    onClick = { filter = "SEALED" },
                    label = { Text(PropertyStrings.filterSealed) },
                )
            }
        }
        item {
            VaultTextField(
                value = query,
                onValueChange = { query = it },
                label = InspectionStrings.search,
                leadingIcon = Icons.Outlined.Search,
            )
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
            rows == null && showLoading -> {
                item {
                    ProgressStatus(true)
                    InspectionRowSkeleton()
                }
                item { InspectionRowSkeleton() }
                item { InspectionRowSkeleton() }
            }
            rows == null -> Unit
            visible.isEmpty() -> {
                item {
                    Text(if (query.isBlank() && filter == "ALL") InspectionStrings.emptyAll else InspectionStrings.emptyFilter, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                items(visible, key = { it.inspection.id }) { overview ->
                    InspectionRow(
                        overview = overview,
                        busy = busy,
                        onResume = { resume(overview.inspection.id) },
                        onReview = { review(overview.inspection.id) },
                        onDelete = { deleting = overview },
                    )
                }
            }
        }
        item { ErrorText(actionError) }
    }

    deleting?.let { target ->
        InspectionDeleteHost(
            container = container,
            overview = target,
            onCommitted = { message ->
                scope.launch {
                    load()
                    feedback = message
                    deleting = null
                }
            },
            onDismiss = { deleting = null },
        )
    }
}
