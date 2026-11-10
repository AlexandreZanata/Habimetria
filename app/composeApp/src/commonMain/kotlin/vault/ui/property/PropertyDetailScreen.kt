package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.STATUS_DRAFT
import vault.local.STATUS_SEALED
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.InspectionRowSkeleton
import vault.ui.ProgressStatus
import vault.ui.SkeletonLine
import vault.ui.SkeletonThumbnail
import vault.ui.Strings
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
            inspections = repo.inspectionsForProperty(propertyId, limit = 50)
            loadError = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local, propertyId) { load() }

    val showLoading = rememberLoadingVisible(property == null || inspections == null)

    if (chooseProfile) InspectionProfileDialog(container, propertyId, onNewInspection) { chooseProfile = false }

    val visible = (inspections.orEmpty()).filter {
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
            showLoading -> {
                ProgressStatus(true)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonThumbnail(88.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonLine(0.6f, 22.dp)
                        SkeletonLine(0.85f)
                    }
                }
                InspectionRowSkeleton()
                InspectionRowSkeleton()
            }
            property == null || inspections == null -> {
                // Grace period: data is on its way, paint nothing yet.
            }
            else -> {
                val current = property!!
                Text(
                    current.nickname.ifBlank { current.address },
                    style = MaterialTheme.typography.titleLarge,
                )
                if (current.nickname.isNotBlank()) {
                    Text(
                        current.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PropertyDataSection(
                    container = container,
                    property = current,
                    onSaved = { scope.launch { load() } },
                )
                if (current.archived) {
                    ArchivedBadge()
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
                }
                Text(PropertyStrings.history, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (visible.isEmpty()) {
                    Text(
                        if (inspections!!.isEmpty()) PropertyStrings.noInspections
                        else PropertyStrings.noMatch,
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
                ErrorText(actionError)
                OutlinedButton(
                    onClick = { chooseProfile = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.newInspection) }
            }
        }
    }
}
