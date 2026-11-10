package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import vault.local.LocalSealer
import vault.local.LocalSession
import vault.local.SEAL_ALREADY
import vault.local.SEAL_EMPTY
import vault.local.SEAL_NOT_DRAFT
import vault.local.SEAL_REPORT_DETAILS
import vault.local.SEAL_UNREADABLE
import vault.local.SealBlockedException
import vault.local.SealEligibility
import vault.local.SealOutcome
import vault.local.db.LocalManifest
import vault.local.platformSigner
import vault.ui.journey.JourneyShortcuts
import vault.ui.journey.SealRecordDetails
import vault.ui.journey.SealStrings
import vault.ui.journey.SealSummary

// LocalSealScreen: honest eligibility first, one tap to freeze the
// immutable snapshot. Double taps and crashes return the same sealed
// manifest; rendering failures later never reopen the inspection.
@Composable
fun LocalSealScreen(
    session: LocalSession,
    inspectionId: String,
    onPdf: () -> Unit,
    onOccurrence: () -> Unit,
    onExitReady: (exitId: String) -> Unit,
    onRoomsShortcut: () -> Unit,
    onReviewShortcut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val signer = remember(session.filesDir) { platformSigner(session.filesDir) }
    val sealer = remember(session, signer) { LocalSealer(session.db, session.originals, signer) }
    var eligibility by remember { mutableStateOf<SealEligibility?>(null) }
    var sealed by remember { mutableStateOf<LocalManifest?>(null) }
    var busy by remember { mutableStateOf(false) }
    var rentalEntry by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inspectionId) {
        try {
            val assessed = withContext(Dispatchers.Default) { sealer.assess(inspectionId) }
            rentalEntry = session.repository.getInspection(inspectionId)?.kind == "entry"
            if (assessed.alreadySealed) sealed = sealer.manifestFor(inspectionId)
            eligibility = assessed
        } catch (e: Exception) {
            error = userErrorMessage(e)
        }
    }

    ScreenColumn {
        JourneyShortcuts(onRooms = onRoomsShortcut, onReview = onReviewShortcut)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val current = eligibility
            val manifest = sealed
            RecordPanel(
                icon = Icons.Filled.Lock,
                title = if (manifest == null) SealStrings.beforeTitle else Strings.reviewSealedTitle,
                body = if (manifest == null) SealStrings.beforeBody else SealStrings.completedBody,
                action = if (manifest == null) null else Strings.generatePdf,
                actionEnabled = !busy,
                onAction = onPdf,
            )
            if (rememberLoadingVisible(current == null && error == null)) {
                LoadingSkeleton {
                    SkeletonCard {
                        SkeletonLine(0.52f, 16.dp)
                        SkeletonInlineActions(84, 65, 75)
                    }
                    SkeletonCard {
                        SkeletonLine(0.45f, 16.dp)
                        SkeletonLine(0.7f, 14.dp)
                        SkeletonLine(0.92f, 14.dp)
                    }
                }
            }
            if (current != null) {
                SealSummary(current)
                if (manifest == null && current.reasons.isNotEmpty()) {
                    ErrorText(current.reasons.joinToString("\n", transform = ::sealReasonText))
                }
                SealRecordDetails(signer.keyId, signer.hardwareBacked, manifest)
            }
            ErrorText(error)
            ProgressStatus(busy)
            if (manifest == null && current?.alreadySealed != true) {
                VaultButton(
                    text = if (busy) Strings.sealing else SealStrings.action,
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                sealed = when (val outcome = withContext(Dispatchers.Default) { sealer.seal(inspectionId) }) {
                                    is SealOutcome.Sealed -> outcome.manifest
                                    is SealOutcome.AlreadySealed -> outcome.manifest
                                }
                            } catch (e: SealBlockedException) {
                                error = e.reasons.joinToString("\n", transform = ::sealReasonText)
                            } catch (e: Exception) {
                                error = userErrorMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && current?.canSeal == true,
                )
            }
            if (manifest != null && rentalEntry) {
                SealSecondaryAction(Strings.exitStart, enabled = !busy, onClick = {
                    scope.launch {
                        try {
                            val inspection = session.repository.getInspection(inspectionId) ?: return@launch
                            val exit = session.repository.startExitInspection(inspection.tenancyId ?: error("tenancy required"))
                            session.navigation.save(vault.local.NavigationProgress(exit.id))
                            onExitReady(exit.id)
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        }
                    }
                })
            }
            SealSecondaryAction(Strings.newOccurrence, enabled = !busy, onClick = onOccurrence)
        }
    }
}

@Composable
private fun SealSecondaryAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
    ) { Text(text) }
}

private fun sealReasonText(reason: String): String = when {
    reason == SEAL_EMPTY -> Strings.sealBlockedEmpty
    reason.startsWith("$SEAL_UNREADABLE:") -> {
        val count = reason.substringAfter(":").toIntOrNull()
        when (count) {
            1 -> "O arquivo original de uma foto está indisponível. Confira as fotos na revisão."
            null -> "Há arquivos originais indisponíveis. Confira as fotos na revisão."
            else -> "Os arquivos originais de $count fotos estão indisponíveis. Confira as fotos na revisão."
        }
    }
    reason == SEAL_ALREADY || reason == SEAL_NOT_DRAFT -> Strings.sealAlreadyDone
    reason == SEAL_REPORT_DETAILS -> SealStrings.reportDetails
    else -> Strings.actionFailed
}
