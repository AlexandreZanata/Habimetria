package vault.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.ReportDeleteOutcome
import vault.local.ReportGenerator
import vault.local.ReportOverview
import vault.local.platformSigner
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.userErrorMessage

// Professional confirmation for report removal: what goes, what stays,
// cancel-safe, busy-guarded, failure keeps context with retry. The
// sealed Manifest and originals never move; "Gerar PDF" rebuilds.
@Composable
fun ReportDeleteHost(
    container: AppContainer,
    overview: ReportOverview,
    onCommitted: (message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = container.local
    var busy by rememberSaveable(overview.report.id) { mutableStateOf(false) }
    var error by rememberSaveable(overview.report.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val generator = remember(session) {
        session?.let {
            ReportGenerator(it.db, it.filesDir, it.originals, it.derivatives, platformSigner(it.filesDir))
        }
    }
    val label = overview.propertyLabel.ifBlank { ReportStrings.unknownProperty }

    fun confirm() {
        scope.launch {
            busy = true
            error = null
            try {
                when (generator?.deleteReport(overview.report.id)) {
                    ReportDeleteOutcome.Deleted -> onCommitted(ReportStrings.deletedOk)
                    ReportDeleteOutcome.Missing, null -> error = Strings.recordMissing
                }
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(ReportStrings.deleteTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    ReportStrings.deleteBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = ::confirm, enabled = !busy) {
                Text(
                    ReportStrings.deleteConfirm,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(Strings.cancel) }
        },
    )
}
