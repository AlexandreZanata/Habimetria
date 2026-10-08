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
    session: LocalSession, renderer: PlanRenderer, inspectionId: String, picker: LocalFilePicker? = null,
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
    var showDeclaration by remember { mutableStateOf(false) }
    var showExternal by remember { mutableStateOf(false) }
    var declarations by remember { mutableStateOf<List<DeclarationCheck>>(emptyList()) }
    var revision by remember { mutableStateOf(0) }
    var showDetails by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    suspend fun load() {
        manifest = generator.manifestFor(inspectionId)
        versions = manifest?.let { generator.versions(it.id) }.orEmpty()
        report = report?.let { current -> versions.firstOrNull { it.id == current.id } } ?: versions.firstOrNull()
        verification = report?.let { withContext(Dispatchers.Default) { generator.verify(it.id) } }
        declarations = report?.takeIf { verification?.allOk() == true }?.let {
            withContext(Dispatchers.Default) { ReportDeclarations(session.db, session.filesDir, platformSigner(session.filesDir)).runCatching { check(it) }.getOrDefault(emptyList()) }
        }.orEmpty()
        if (manifest == null) error = Strings.pdfSealFirst
    }
    LaunchedEffect(inspectionId, revision) {
        try { busy = true; load() } catch (e: Exception) { error = userErrorMessage(e) } finally { busy = false; loaded = true }
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
    if (showDeclaration && report != null) DeclarationSheet(session, report!!.id, picker) { showDeclaration = false; revision++ }
    if (showExternal && report != null) ExternalSigningSheet(session, report!!.id, picker) { showExternal = false; revision++ }
    ScreenColumn {
        ScreenTitle(Strings.reportPdf)
        // The PDF exists only on sealed inspections and Rooms redirects
        // those back to Review, so route there directly instead of
        // bouncing off Rooms.
        vault.ui.report.PdfJourneySegments(
            onRooms = { if (manifest != null) onReviewShortcut() else onRoomsShortcut() },
            onReview = onReviewShortcut,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val current = report
            if (!loaded) {
                vault.ui.ProgressStatus(true)
                vault.ui.report.PdfStatusSkeleton()
                vault.ui.report.PdfOptionSkeleton()
                vault.ui.report.PdfOptionSkeleton()
                vault.ui.report.PdfOptionSkeleton()
            }
            if (loaded) {
            val ready = current?.status == REPORT_READY && verification?.allOk() == true
            if (ready) {
                vault.ui.report.PdfStatusCard(
                    versionNumber = current!!.versionNumber,
                    shareEnabled = !busy,
                    onShare = { share(false) },
                )
            }
            if (current?.status == REPORT_READY && verification != null && !ready) {
                Text("A verificação detectou arquivo ausente ou alterado. O compartilhamento está bloqueado.", color = MaterialTheme.colorScheme.error)
            }
            progress?.let { (done, total) ->
                LinearProgressIndicator(progress = { if (total == 0) 0f else done / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("$done/$total páginas")
            }
            ErrorText(error)
            if (!busy && current?.status != REPORT_READY) VaultButton(Strings.pdfGenerate, { generate(false) }, enabled = manifest != null)
            if (busy) VaultButton(Strings.pdfCancel, { cancelled.value = true })
            vault.ui.report.PdfSectionHeader(Strings.pdfMoreOptions, Strings.pdfMoreOptionsBody)
            vault.ui.report.PdfOptionCard(
                vault.ui.report.PdfOptionIcons.zip,
                Strings.pdfPackageTitle,
                Strings.pdfPackageBody,
                enabled = !busy && ready,
                onClick = { share(true) },
            )
            vault.ui.report.PdfOptionCard(
                vault.ui.report.PdfOptionIcons.authorship,
                Strings.pdfAuthorshipTitle,
                Strings.pdfAuthorshipBody,
                enabled = !busy && ready,
                onClick = { showDeclaration = true },
            )
            vault.ui.report.PdfOptionCard(
                vault.ui.report.PdfOptionIcons.external,
                Strings.pdfExternalTitle,
                Strings.pdfExternalBody,
                enabled = !busy && ready,
                onClick = { showExternal = true },
            )
            declarations.forEach { DeclarationStatus(it) }
            vault.ui.report.PdfSectionDivider()
            vault.ui.report.PdfSectionHeader(Strings.pdfMoreActions, Strings.pdfMoreActionsBody)
            if (current?.status == REPORT_READY) {
                vault.ui.report.PdfOptionCard(
                    vault.ui.report.PdfOptionIcons.newVersion,
                    Strings.pdfNewVersionTitle,
                    Strings.pdfNewVersionBody,
                    enabled = !busy,
                    onClick = { generate(true) },
                )
            }
            vault.ui.report.PdfOptionCard(
                vault.ui.report.PdfOptionIcons.offline,
                Strings.verifyTitle,
                Strings.pdfVerifyBody,
                enabled = !busy,
                onClick = onVerify,
            )
            if (versions.size > 1) {
                Text("Histórico de versões", style = MaterialTheme.typography.titleMedium)
                versions.forEach { version ->
                    TextButton(enabled = !busy, onClick = {
                        declarations = emptyList(); report = version; verification = null; busy = true
                        scope.launch {
                            try { verification = withContext(Dispatchers.Default) { generator.verify(version.id) }; load() }
                            catch (e: Exception) { error = userErrorMessage(e) }
                            finally { busy = false }
                        }
                    }) { Text("Versão ${version.versionNumber} · ${version.status} · ${version.createdAtDevice}") }
                }
            }
            Text("O ZIP inclui PDF, recibo, manifesto, chave pública, declarações registradas, originais e os dados pessoais completos fornecidos. Permite verificar a integridade offline; a identidade do autor continua declarada.", style = MaterialTheme.typography.bodySmall)
            Text("PAdES/Gov.br/ACT são etapas externas opcionais com ação explícita. O app não incorpora PAdES nem promete aceitação jurídica; cópias externas exigem conferência no verificador oficial.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { showDetails = !showDetails }) { Text(Strings.technicalDetails) }
            if (showDetails && current != null) {
                Text("Relatório: ${current.id}\nManifesto: ${current.manifestSha256}\nPDF: ${current.pdfSha256.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                verification?.let { Text("PDF: ${it.pdfIntact} · manifesto: ${it.manifestOk} · recibo: ${it.receiptOk} · originais: ${it.originalsIntact} · declarações: ${it.declarationsIntact}", style = MaterialTheme.typography.bodySmall) }
            }
            }
        }
    }
}
