package vault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import vault.local.LocalSealer
import vault.local.LocalSession
import vault.local.SEAL_ALREADY
import vault.local.SEAL_EMPTY
import vault.local.SEAL_NOT_DRAFT
import vault.local.SEAL_UNREADABLE
import vault.local.SealBlockedException
import vault.local.SealEligibility
import vault.local.SealOutcome
import vault.local.db.LocalManifest
import vault.local.platformSigner

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
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val signer = remember(session.filesDir) { platformSigner(session.filesDir) }
    val sealer = remember(session, signer) { LocalSealer(session.db, session.originals, signer) }
    var eligibility by remember { mutableStateOf<SealEligibility?>(null) }
    var sealed by remember { mutableStateOf<LocalManifest?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                eligibility = sealer.assess(inspectionId)
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.sealTitle)
        BodyText(Strings.sealWarning)
        val current = eligibility
        if (current != null) {
            Text("${Strings.rooms}: ${current.rooms} · ${Strings.evidence}: ${current.evidences} · ${Strings.findingsList}: ${current.findings}")
            if (current.gaps.any { it.percent < 100 }) {
                Text("${Strings.sealIncomplete}:", style = MaterialTheme.typography.titleMedium)
                current.gaps.filter { it.percent < 100 }.forEach { gap ->
                    Text(
                        "${gap.label} ${gap.percent}% — ${Strings.missingShots}: " +
                            gap.missing.joinToString(", ") { shotLabel(it) },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (current.unreadable.isNotEmpty()) {
                Text(
                    "${Strings.sealBlockedUnreadable}: ${current.unreadable.size}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                "${Strings.sealKeyLabel}: ${signer.keyId.take(16)}… " +
                    "(${if (signer.hardwareBacked) Strings.sealHardware else Strings.sealSoftware})",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(Strings.sealTimeLabel, style = MaterialTheme.typography.bodyMedium)
        }
        sealed?.let {
            Text("${Strings.sealed} ✓")
            Text("${Strings.sealManifestLabel}: ${it.canonicalSha256.take(16)}…")
            VaultButton(text = "${Strings.generatePdf} →", onClick = onPdf)
            VaultButton(
                text = "${Strings.exitStart} →",
                onClick = {
                    scope.launch {
                        try {
                            val inspection = session.repository.getInspection(inspectionId)
                                ?: return@launch
                            val exit = session.repository.startExitInspection(inspection.tenancyId)
                            session.navigation.save(
                                vault.local.NavigationProgress(exit.id),
                            )
                            onExitReady(exit.id)
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        }
                    }
                },
                enabled = !busy,
            )
        }
        VaultButton(text = "${Strings.newOccurrence} →", onClick = onOccurrence)
        ErrorText(error)
        if (sealed == null && eligibility?.alreadySealed != true) {
            VaultButton(
                text = if (busy) Strings.sealing else Strings.sealNow,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            sealed = when (val outcome = sealer.seal(inspectionId)) {
                                is SealOutcome.Sealed -> outcome.manifest
                                is SealOutcome.AlreadySealed -> outcome.manifest
                            }
                            refresh()
                        } catch (e: SealBlockedException) {
                            error = e.reasons.joinToString("; ") { sealReasonText(it) }
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            )
        }
        VaultButton(text = Strings.back, onClick = onBack)
    }
}

private fun sealReasonText(reason: String): String = when {
    reason == SEAL_EMPTY -> Strings.sealBlockedEmpty
    reason.startsWith(SEAL_UNREADABLE) -> "${Strings.sealBlockedUnreadable}: ${reason.substringAfter(":")}"
    reason == SEAL_ALREADY -> Strings.sealAlreadyDone
    reason == SEAL_NOT_DRAFT -> Strings.sealAlreadyDone
    else -> reason
}
