package vault.ui

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.launch
import vault.local.IncompleteReportException
import vault.local.LocalSession
import vault.local.PlanRenderer
import vault.local.RenderCancelledException
import vault.local.ReportGenerator
import vault.local.ReportVerification
import vault.local.db.LocalManifest
import vault.local.db.LocalReport
import vault.local.platformSigner

// LocalPdfScreen: generate → save → hash → receipt → share, all offline.
// Bytes land on disk before sharing; the hash is computed over the exact
// saved bytes. Cancellation and missing originals fail without reopening.
@Composable
fun LocalPdfScreen(
    session: LocalSession,
    renderer: PlanRenderer,
    inspectionId: String,
    onSharePdf: (path: String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val signer = remember(session.filesDir) { platformSigner(session.filesDir) }
    val generator = remember(session, signer) {
        ReportGenerator(session.db, session.filesDir, session.originals, session.derivatives, signer)
    }
    var manifest by remember { mutableStateOf<LocalManifest?>(null) }
    var report by remember { mutableStateOf<LocalReport?>(null) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var verification by remember { mutableStateOf<ReportVerification?>(null) }
    var busy by remember { mutableStateOf(false) }
    var cancelled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                val man = generator.manifestFor(inspectionId)
                manifest = man
                report = man?.let { generator.existingReport(it.id) }
                error = if (man == null) Strings.pdfSealFirst else null
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    LaunchedEffect(inspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.reportPdf)
        val current = report
        val pdfHash = current?.pdfSha256
        if (current != null && pdfHash != null) {
            Text(Strings.pdfReady)
            Text("${Strings.sealManifestLabel}: ${current.manifestSha256.take(16)}…")
            Text("${Strings.pdfHash}: ${pdfHash.take(16)}…")
            Text("${Strings.pdfReceipt}: ${current.receiptSignature?.take(16)}…")
        }
        progress?.let { (done, total) ->
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("$done/$total")
        }
        verification?.let {
            Text(
                if (it.allOk()) "✓ ${Strings.pdfChecks}"
                else "PDF:${it.pdfIntact} manifesto:${it.manifestOk} recibo:${it.receiptOk}",
            )
        }
        ErrorText(error)
        if (!busy && (current == null || current.pdfSha256 == null)) {
            VaultButton(
                text = Strings.pdfGenerate,
                onClick = {
                    busy = true
                    cancelled = false
                    progress = 0 to 0
                    error = null
                    verification = null
                    scope.launch {
                        try {
                            report = generator.generate(
                                inspectionId = inspectionId,
                                renderer = renderer,
                                onProgress = { done, total -> progress = done to total },
                                isCancelled = { cancelled },
                            )
                            progress = null
                        } catch (_: RenderCancelledException) {
                            error = Strings.pdfCancelled
                            progress = null
                        } catch (_: IncompleteReportException) {
                            error = Strings.pdfIncomplete
                            progress = null
                        } catch (e: Exception) {
                            error = e.message?.take(200)
                            progress = null
                        } finally {
                            busy = false
                            refresh()
                        }
                    }
                },
                enabled = manifest != null,
            )
        }
        if (busy) {
            VaultButton(text = Strings.pdfCancel, onClick = { cancelled = true })
        }
        VaultButton(
            text = Strings.sharePdf,
            onClick = { current?.let { onSharePdf(generator.pdfPath(it.id)) } },
            enabled = !busy && current?.pdfSha256 != null,
        )
        VaultButton(
            text = Strings.pdfVerify,
            onClick = {
                scope.launch {
                    try {
                        current?.let { verification = generator.verify(it.id) }
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                    }
                }
            },
            enabled = !busy && current?.pdfSha256 != null,
        )
        VaultButton(text = Strings.back, onClick = onBack)
    }
}
