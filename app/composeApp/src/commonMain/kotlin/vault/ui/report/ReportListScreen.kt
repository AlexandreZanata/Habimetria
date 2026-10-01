package vault.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.PendingPdf
import vault.local.REPORT_READY
import vault.local.ReportOverview
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.Strings
import vault.ui.VaultTextField
import vault.ui.property.PropertyStrings
import vault.ui.userErrorMessage

// Relatórios grouped by property: finalized READY reports with
// open/verify entries, plus SEALED inspections awaiting a PDF with
// an explicit "Gerar PDF" entry. Pending never claims a report
// exists; DRAFT never offers generation (seal eligibility). All
// actions work offline through the on-device pipeline.
@Composable
fun ReportListScreen(
    container: AppContainer,
    onOpenPdf: (inspectionId: String) -> Unit,
    onVerify: (inspectionId: String) -> Unit,
    onGenerate: (inspectionId: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var reports by remember { mutableStateOf<List<ReportOverview>?>(null) }
    var pending by remember { mutableStateOf<List<PendingPdf>?>(null) }
    var loadError by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val local = container.local

    suspend fun load() {
        val overviews = local?.overviews ?: return
        try {
            reports = overviews.reportOverviews(limit = 100).filter { it.report.status == REPORT_READY }
            pending = overviews.pendingPdfs(limit = 100)
            loadError = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local) { load() }

    fun matches(label: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || label.lowercase().contains(q)
    }

    val ready = (reports.orEmpty()).filter { matches(it.propertyLabel) }
    val awaiting = (pending.orEmpty()).filter { matches(it.propertyLabel) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            VaultTextField(
                value = query,
                onValueChange = { query = it },
                label = ReportStrings.search,
            )
        }
        when {
            loadError != null -> {
                item {
                    ErrorText(loadError)
                    OutlinedButton(onClick = { scope.launch { load() } }) { Text(Strings.retry) }
                }
            }
            reports == null || pending == null -> {
                item {
                    ProgressStatus(true)
                    Text(Strings.loadingProperties)
                }
            }
            ready.isEmpty() && awaiting.isEmpty() -> {
                item {
                    Text(if (query.isBlank()) ReportStrings.emptyAll else ReportStrings.emptySearch)
                }
            }
            else -> {
                if (ready.isNotEmpty()) {
                    item {
                        Text(ReportStrings.finalized, style = MaterialTheme.typography.titleMedium)
                    }
                    ready.groupBy { it.propertyId }.forEach { (_, group) ->
                        item {
                            Text(
                                group.first().propertyLabel.ifBlank { ReportStrings.unknownProperty },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(group, key = { it.report.id }) { overview ->
                            FinalizedRow(
                                overview = overview,
                                onOpen = { onOpenPdf(overview.inspectionId) },
                                onVerify = { onVerify(overview.inspectionId) },
                            )
                        }
                    }
                }
                if (awaiting.isNotEmpty()) {
                    item {
                        Text(ReportStrings.pending, style = MaterialTheme.typography.titleMedium)
                    }
                    awaiting.groupBy { it.propertyId }.forEach { (_, group) ->
                        item {
                            Text(
                                group.first().propertyLabel.ifBlank { ReportStrings.unknownProperty },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(group, key = { it.inspectionId }) { item ->
                            OutlinedButton(
                                onClick = { onGenerate(item.inspectionId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(Strings.generatePdf) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalizedRow(
    overview: ReportOverview,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
            Text(PropertyStrings.openDetail)
        }
        OutlinedButton(onClick = onVerify, modifier = Modifier.weight(1f)) {
            Text(Strings.pdfVerify)
        }
    }
}
