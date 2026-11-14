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
import vault.ui.journey.JourneyShortcuts
import vault.ui.report.PdfOptionIcons
import vault.ui.report.PdfOptionRow
import vault.ui.report.PdfOptionsGroup
import vault.ui.report.PdfSectionDivider
import vault.ui.report.PdfStatusCard
import vault.ui.report.ReportStrings
import vault.ui.review.PdfOptionsSkeleton

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
    var rendering by remember { mutableStateOf(false) }
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
        busy = true
        rendering = true
        cancelled.value = false
        error = null
        verification = null
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
            finally {
                rendering = false
                busy = false
                progress = null
            }
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
        // Rooms redirects sealed inspections back to Review.
        JourneyShortcuts(
            onRooms = if (manifest != null) onReviewShortcut else onRoomsShortcut,
            onReview = onReviewShortcut,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (rememberLoadingVisible(!loaded)) {
                LoadingSkeleton(spacing = 16.dp) {
                    RecordPanelSkeleton(featured = true)
                    PdfOptionsSkeleton()
                }
            }
            if (!loaded) return@Column

            val current = report
            val ready = current?.status == REPORT_READY && verification?.allOk() == true
            if (ready) {
                PdfStatusCard(current.versionNumber, shareEnabled = !busy, onShare = { share(false) })
            }
            if (current?.status == REPORT_READY && verification != null && !ready) {
                ErrorText(ReportStrings.integrityFailure)
            }
            progress?.let { (done, total) ->
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$done/$total páginas")
            }
            ErrorText(error)
            ProgressStatus(busy && !rendering)
            if (!busy && current?.status != REPORT_READY) {
                VaultButton(Strings.pdfGenerate, { generate(false) }, enabled = manifest != null)
            }
            if (rendering) VaultButton(Strings.pdfCancel, { cancelled.value = true })

            PdfOptionsGroup(ReportStrings.documentsSection) {
                PdfOptionRow(PdfOptionIcons.zip, ReportStrings.packageTitle, ReportStrings.packageBody, !busy && ready) {
                    share(true)
                }
                PdfSectionDivider()
                PdfOptionRow(PdfOptionIcons.authorship, ReportStrings.declarationsTitle, ReportStrings.declarationsBody, !busy && ready) {
                    showDeclaration = true
                }
                PdfSectionDivider()
                PdfOptionRow(PdfOptionIcons.external, ReportStrings.externalTitle, ReportStrings.externalBody, !busy && ready) {
                    showExternal = true
                }
            }
            declarations.forEach { DeclarationStatus(it) }
            PdfOptionsGroup(ReportStrings.verificationSection) {
                PdfOptionRow(PdfOptionIcons.offline, Strings.verifyTitle, ReportStrings.verifyBody, !busy, onVerify)
                if (current?.status == REPORT_READY) {
                    PdfSectionDivider()
                    PdfOptionRow(PdfOptionIcons.newVersion, ReportStrings.newVersionTitle, ReportStrings.newVersionBody, !busy) {
                        generate(true)
                    }
                }
            }
            if (versions.size > 1) {
                Text("Histórico de versões", style = MaterialTheme.typography.titleMedium)
                versions.forEach { version ->
                    TextButton(enabled = !busy, onClick = {
                        declarations = emptyList()
                        report = version
                        verification = null
                        busy = true
                        scope.launch {
                            try {
                                load()
                            } catch (e: Exception) {
                                error = userErrorMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    }) { Text("Versão ${version.versionNumber} · ${version.status} · ${version.createdAtDevice}") }
                }
            }
            PdfOptionsGroup(ReportStrings.notesTitle) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ReportStrings.packageNotes, style = MaterialTheme.typography.bodySmall)
                    Text(ReportStrings.externalNotes, style = MaterialTheme.typography.bodySmall)
                    if (current != null) {
                        TextButton(onClick = { showDetails = !showDetails }) {
                            Text(if (showDetails) ReportStrings.hideTechnicalDetails else Strings.technicalDetails)
                        }
                        if (showDetails) {
                            Text("Relatório: ${current.id}\nManifesto: ${current.manifestSha256}\nPDF: ${current.pdfSha256.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                            verification?.let {
                                Text("PDF: ${it.pdfIntact} · manifesto: ${it.manifestOk} · recibo: ${it.receiptOk} · originais: ${it.originalsIntact} · declarações: ${it.declarationsIntact}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
