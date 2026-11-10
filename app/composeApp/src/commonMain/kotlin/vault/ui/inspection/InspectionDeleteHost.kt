package vault.ui.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import vault.local.InspectionOverview
import vault.local.LocalSession
import vault.local.STATUS_SEALED
import vault.local.db.InspectionDeleteOutcome
import vault.local.db.InspectionRemovalPlan
import vault.ui.AppContainer
import vault.ui.Strings
import vault.ui.userErrorMessage

// Inspection removal flow: fresh eligibility wording (draft, sealed or
// blocked), commit first and reload after. Cancel is side-effect free;
// failure keeps the dialog context with retry. Navigation refs to the
// removed inspection drop; everything else stays untouched.
@Composable
fun InspectionDeleteHost(
    container: AppContainer,
    overview: InspectionOverview,
    onCommitted: (message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = container.local
    val inspectionId = overview.inspection.id
    val sealed = overview.inspection.status == STATUS_SEALED
    var blocked by rememberSaveable(inspectionId) { mutableStateOf(false) }
    var busy by remember(inspectionId) { mutableStateOf(false) }
    var commitError by rememberSaveable(inspectionId) { mutableStateOf<String?>(null) }
    var missing by rememberSaveable(inspectionId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session, inspectionId) {
        val removal = session?.inspectionRemoval ?: return@LaunchedEffect
        try {
            when (removal.eligibility(inspectionId)) {
                InspectionRemovalPlan.Missing -> missing = true
                is InspectionRemovalPlan.Blocked -> blocked = true
                is InspectionRemovalPlan.Ready -> Unit
            }
        } catch (e: Exception) {
            commitError = userErrorMessage(e)
        }
    }

    fun confirm() {
        if (busy || blocked || missing || session == null) return
        busy = true
        commitError = null
        scope.launch {
            try {
                val removal = session.inspectionRemoval
                when (removal.delete(inspectionId)) {
                    InspectionDeleteOutcome.Deleted -> {
                        revalidateAfterInspectionDelete(session, inspectionId)
                        onCommitted(InspectionStrings.deletedOk)
                    }
                    is InspectionDeleteOutcome.Blocked -> blocked = true
                    InspectionDeleteOutcome.Missing -> missing = true
                }
            } catch (e: Exception) {
                commitError = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    InspectionDeleteDialog(
        propertyLabel = overview.propertyLabel.ifBlank { InspectionStrings.unknownProperty },
        sealed = sealed,
        message = when {
            missing -> Strings.recordMissing
            blocked -> InspectionStrings.blockedBody
            sealed -> InspectionStrings.deleteSealedBody
            else -> InspectionStrings.deleteDraftBody
        },
        error = commitError,
        busy = busy,
        confirmEnabled = session != null && !busy && !missing && !blocked,
        onConfirm = ::confirm,
        onDismiss = onDismiss,
    )
}

private suspend fun revalidateAfterInspectionDelete(
    session: LocalSession,
    deletedInspectionId: String,
) {
    val progress = session.navigation.load()
    if (progress.inspectionId != deletedInspectionId) return
    session.navigation.save(
        progress.copy(
            inspectionId = null,
            roomId = null,
            roomLabel = null,
        ),
    )
}
