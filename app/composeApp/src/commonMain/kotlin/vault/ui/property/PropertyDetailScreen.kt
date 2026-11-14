package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.STATUS_DRAFT
import vault.local.STATUS_SEALED
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.LoadingSkeleton
import vault.ui.PropertyDetailSkeleton
import vault.ui.RecordPanel
import vault.ui.Strings
import vault.ui.inspection.InspectionStatusFilters
import vault.ui.journey.JourneyPrimaryAction
import vault.ui.rememberLoadingVisible
import vault.ui.userErrorMessage

// Property detail: address/nickname plus bounded inspection history.
// DRAFT resumes capture/review; SEALED opens read-only review.
// Filter persists across rotation; loading/error/empty differ with retry.
@Composable
fun PropertyDetailScreen(
    container: AppContainer,
    propertyId: String,
    onResume: (inspectionId: String) -> Unit,
    onReview: (inspectionId: String) -> Unit,
    onNewInspection: (inspectionId: String) -> Unit,
) {
    var property by remember { mutableStateOf<LocalProperty?>(null) }
    var inspections by remember { mutableStateOf<List<LocalInspection>?>(null) }
    var loadError by rememberSaveable { mutableStateOf<String?>(null) }
    var actionError by rememberSaveable { mutableStateOf<String?>(null) }
    var chooseProfile by remember { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val scope = rememberCoroutineScope()
    val local = container.local

    suspend fun load() {
        val repo = local?.repository ?: return
        try {
            property = repo.getProperty(propertyId)
            if (property == null) {
                loadError = PropertyStrings.propertyNotFound
                return
            }
            inspections = repo.inspectionsForProperty(propertyId, limit = 50)
            loadError = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local, propertyId) { load() }

    val showLoading = rememberLoadingVisible((property == null || inspections == null) && loadError == null)

    if (chooseProfile) InspectionProfileDialog(container, propertyId, onNewInspection) { chooseProfile = false }

    val current = property
    val history = inspections
    val visible = history.orEmpty().filter {
        when (filter) {
            "DRAFT" -> it.status == STATUS_DRAFT
            "SEALED" -> it.status == STATUS_SEALED
            else -> true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            loadError != null -> {
                ErrorText(loadError)
                OutlinedButton(onClick = { scope.launch { load() } }) { Text(Strings.retry) }
            }
            showLoading -> LoadingSkeleton { PropertyDetailSkeleton() }
            current == null || history == null -> {
                // Grace period: data is on its way, paint nothing yet.
            }
            else -> {
                PropertyDetailHeader(container, current, onCoverSaved = { scope.launch { load() } })
                if (current.archived) {
                    Text(
                        PropertyStrings.archivedDetailNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RestoreButton(busy = busy) {
                        busy = true
                        actionError = null
                        scope.launch {
                            try {
                                local?.propertyRemoval?.restore(propertyId)
                                load()
                            } catch (e: Exception) {
                                actionError = userErrorMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    }
                } else {
                    JourneyPrimaryAction(
                        Strings.newInspection,
                        icon = Icons.Filled.Add,
                        onClick = { chooseProfile = true },
                        enabled = !busy,
                    )
                }
                ErrorText(actionError)
                PropertyDataSection(
                    container = container,
                    property = current,
                    onSaved = { scope.launch { load() } },
                )
                Text(
                    PropertyStrings.history,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                if (history.isNotEmpty()) {
                    InspectionStatusFilters(selected = filter, onSelect = { filter = it })
                }
                if (visible.isEmpty()) {
                    RecordPanel(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        title = if (history.isEmpty()) PropertyStrings.noInspections else PropertyStrings.noMatch,
                        body = when {
                            history.isNotEmpty() -> PropertyStrings.historyFilterHint
                            current.archived -> PropertyStrings.archivedHistoryHint
                            else -> PropertyStrings.emptyHistoryHint
                        },
                    )
                }
                visible.forEach { inspection ->
                    InspectionRow(
                        inspection = inspection,
                        busy = busy,
                        onResume = { onResume(inspection.id) },
                        onReview = { onReview(inspection.id) },
                    )
                }
            }
        }
    }
}
