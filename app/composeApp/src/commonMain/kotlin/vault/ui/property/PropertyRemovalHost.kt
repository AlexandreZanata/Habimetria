package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.PropertyOverview
import vault.local.db.PropertyArchiveOutcome
import vault.local.db.PropertyDeleteOutcome
import vault.local.db.PropertyEligibility
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.userErrorMessage

// U08 removal flow for the Imóveis list. Fetches a fresh eligibility
// snapshot for Excluir vs Arquivar wording, commits first and reloads
// after. Cancel is side-effect free; failure keeps the dialog context
// with retry. The card stays mounted until a committed reload, so
// assistive focus returns to a stable context (device AT pass: U10).
@Composable
fun PropertyRemovalHost(
    container: AppContainer,
    overview: PropertyOverview,
    onCommitted: (message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = container.local
    val propertyId = overview.property.id
    val label = overview.property.nickname.ifBlank { overview.property.address }
    var eligibility by remember(propertyId) { mutableStateOf<PropertyEligibility?>(null) }
    var fetchError by rememberSaveable(propertyId) { mutableStateOf<String?>(null) }
    var busy by rememberSaveable(propertyId) { mutableStateOf(false) }
    var commitError by rememberSaveable(propertyId) { mutableStateOf<String?>(null) }
    var raced by rememberSaveable(propertyId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun fetch() {
        val removal = session?.propertyRemoval ?: return
        try {
            eligibility = removal.eligibility(propertyId)
            fetchError = null
        } catch (e: Exception) {
            fetchError = userErrorMessage(e)
        }
    }

    LaunchedEffect(session, propertyId) { fetch() }

    fun confirm() {
        val snapshot = eligibility ?: return
        scope.launch {
            busy = true
            commitError = null
            try {
                val removal = session?.propertyRemoval ?: return@launch
                if (snapshot.canDelete && !raced) {
                    when (removal.deleteEmpty(propertyId)) {
                        PropertyDeleteOutcome.Deleted -> {
                            revalidateNavigationAfterDelete(session, propertyId)
                            onCommitted(PropertyStrings.deletedOk)
                        }
                        is PropertyDeleteOutcome.Blocked -> {
                            raced = true
                            eligibility = removal.eligibility(propertyId)
                            commitError = PropertyStrings.blockedByHistory
                        }
                        PropertyDeleteOutcome.Missing -> {
                            commitError = Strings.recordMissing
                        }
                    }
                } else {
                    when (removal.archive(propertyId)) {
                        PropertyArchiveOutcome.Archived,
                        PropertyArchiveOutcome.AlreadyArchived,
                        -> onCommitted(PropertyStrings.archivedOk)
                        PropertyArchiveOutcome.Missing -> {
                            commitError = Strings.recordMissing
                        }
                    }
                }
            } catch (e: Exception) {
                commitError = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    val snapshot = eligibility
    when {
        snapshot == null && fetchError != null -> {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorText(fetchError)
                OutlinedButton(onClick = { scope.launch { fetch() } }) { Text(Strings.retry) }
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            }
        }
        snapshot != null -> {
            val deleting = snapshot.canDelete && !raced
            PropertyRemovalDialog(
                propertyLabel = label,
                headline = if (deleting) PropertyStrings.deleteTitle else PropertyStrings.archiveTitle,
                body = if (deleting) PropertyStrings.deleteBody else PropertyStrings.archiveBody,
                confirmLabel = if (deleting) {
                    PropertyStrings.deleteConfirm
                } else {
                    PropertyStrings.archiveConfirm
                },
                busy = busy,
                error = commitError,
                onConfirm = ::confirm,
                onDismiss = onDismiss,
            )
        }
    }
}

// Success feedback after a committed removal. Polite live region so
// screen readers announce Excluído/Arquivado without moving focus.
@Composable
fun RemovalFeedback(message: String) {
    Text(
        message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
