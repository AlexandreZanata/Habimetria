package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import vault.local.*
import vault.local.db.LocalManifest
import vault.local.db.LocalReport

@Composable
fun LocalPdfScreen(
    session: LocalSession, renderer: PlanRenderer, inspectionId: String,
    onShare: (path: String, mime: String) -> Unit, onVerify: () -> Unit,
    onRoomsShortcut: () -> Unit, onReviewShortcut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val generator = remember(session) { ReportGenerator(session.db, session.filesDir, session.originals,
        session.derivatives, platformSigner(session.filesDir)) }
    var manifest by remember { mutableStateOf<LocalManifest?>(null) }
    var versions by remember { mutableStateOf<List<LocalReport>>(emptyList()) }
    var report by remember { mutableStateOf<LocalReport?>(null) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var verification by remember { mutableStateOf<ReportVerification?>(null) }
    var busy by remember { mutableStateOf(false) }
    val cancelled = remember { MutableStateFlow(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    suspend fun load() {
        manifest = generator.manifestFor(inspectionId)
        versions = manifest?.let { generator.versions(it.id) }.orEmpty()
        report = report?.let { current -> versions.firstOrNull { it.id == current.id } } ?: versions.firstOrNull()
        verification = report?.let { withContext(Dispatchers.Default) { generator.verify(it.id) } }
        if (manifest == null) error = Strings.pdfSealFirst
    }
    LaunchedEffect(inspectionId) {
        try { busy = true; load() } catch (e: Exception) { error = userErrorMessage(e) } finally { busy = false }
    }
    fun generate(newVersion: Boolean) {
        busy = true; cancelled.value = false; error = null; verification = null
        scope.launch {
            try {
                report = withContext(Dispatchers.Default) {
                    generator.generate(inspectionId, renderer,
                        onProgress = { done, total -> scope.launch { progress = done to total } },
                        isCancelled = { cancelled.value }, newVersion = newVersion)
                }
                load()
            } catch (_: RenderCancelledException) { error = Strings.pdfCancelled }
            catch (_: IncompleteReportException) { error = Strings.pdfIncomplete }
            catch (e: Exception) { error = userErrorMessage(e) }
            finally { busy = false; progress = null }
        }
    }
    fun share(packageFile: Boolean) {
        val selected = report ?: return
        busy = true; error = null
        scope.launch {
            try {
                val path = withContext(Dispatchers.Default) {
                    val checked = generator.verify(selected.id)
                    verification = checked
                    check(selected.status == REPORT_READY && checked.allOk()) { "report verification failed" }
                    if (packageFile) generator.exportZip(selected.id) else generator.pdfPath(selected.id)
                }
                onShare(path, if (packageFile) "application/zip" else "application/pdf")
            } catch (e: Exception) { error = userErrorMessage(e) }
            finally { busy = false }
        }
    }
    ScreenColumn {
        ScreenTitle(Strings.reportPdf)
        vault.ui.journey.JourneyShortcuts(onRooms = onRoomsShortcut, onReview = onReviewShortcut)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val current = report
            val ready = current?.status == REPORT_READY && verification?.allOk() == true
            if (ready) Text("${Strings.pdfReady} · versão ${current!!.versionNumber}", style = MaterialTheme.typography.titleMedium)
            if (current?.status == REPORT_READY && verification != null && !ready) {
                Text("A verificação detectou arquivo ausente ou alterado. O compartilhamento está bloqueado.", color = MaterialTheme.colorScheme.error)
            }
            if (versions.size > 1) {
                Text("Histórico de versões", style = MaterialTheme.typography.titleMedium)
                versions.forEach { version ->
                    TextButton(enabled = !busy, onClick = {
                        report = version; verification = null; busy = true
                        scope.launch {
                            try { verification = withContext(Dispatchers.Default) { generator.verify(version.id) } }
                            catch (e: Exception) { error = userErrorMessage(e) }
                            finally { busy = false }
                        }
                    }) { Text("Versão ${version.versionNumber} · ${version.status} · ${version.createdAtDevice}") }
                }
            }
            progress?.let { (done, total) ->
                LinearProgressIndicator(progress = { if (total == 0) 0f else done / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("$done/$total páginas")
            }
            ErrorText(error)
            if (!busy && current?.status != REPORT_READY) VaultButton(Strings.pdfGenerate, { generate(false) }, enabled = manifest != null)
            if (busy) VaultButton(Strings.pdfCancel, { cancelled.value = true })
            VaultButton(Strings.sharePdf, { share(false) }, enabled = !busy && ready)
            VaultButton("Compartilhar pacote verificável (ZIP)", { share(true) }, enabled = !busy && ready)
            Text("O ZIP inclui PDF, recibo, manifesto, chave pública e originais. Permite verificar a integridade offline; a identidade do autor continua declarada.", style = MaterialTheme.typography.bodySmall)
            if (current?.status == REPORT_READY) VaultButton("Gerar nova versão", { generate(true) }, enabled = !busy)
            VaultButton(Strings.verifyTitle, onVerify, enabled = !busy)
            TextButton(onClick = { showDetails = !showDetails }) { Text(Strings.technicalDetails) }
            if (showDetails && current != null) {
                Text("Relatório: ${current.id}\nManifesto: ${current.manifestSha256}\nPDF: ${current.pdfSha256.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                verification?.let { Text("PDF: ${it.pdfIntact} · manifesto: ${it.manifestOk} · recibo: ${it.receiptOk} · originais: ${it.originalsIntact}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
