package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.api.VerifyResult
import vault.pdf.DocPhoto
import vault.pdf.PdfDoc
import vault.pdf.RoomSection
import vault.pdf.SealBlock
import vault.queue.QueueStatus

// Pdf: two-pass seal flow. Generates on-device, uploads the final,
// compares local vs server SHA, then shares. Never "saved" before ack.
@Composable
fun PdfScreen(
    container: AppContainer,
    inspectionId: String,
    reportId: String,
    verifyCode: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var verifyUrl by remember { mutableStateOf("") }
    var manifestSha by remember { mutableStateOf("") }
    var sealedAt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready.") }
    var busy by remember { mutableStateOf(false) }
    var pdfPath by remember { mutableStateOf<String?>(null) }
    var check by remember { mutableStateOf<VerifyResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reportId) {
        try {
            val detail = container.api.reportDetail(reportId)
            verifyUrl = detail.verifyUrl
            manifestSha = detail.report.manifestSha256
            sealedAt = detail.report.sealedAt
        } catch (e: Exception) {
            error = e.message?.take(160)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Report PDF")
        Text("Code: $verifyCode")
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(status)
        error?.let { Text("Error: $it") }
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        status = "Reading photos…"
                        val rooms = container.api.listRooms(inspectionId)
                        val queue = container.store.load().filter {
                            it.inspectionId == inspectionId && it.status == QueueStatus.CONFIRMED
                        }
                        val sections = rooms.map { room ->
                            val photos = queue.filter { it.roomId == room.id }.map { item ->
                                val bytes = withContext(Dispatchers.Default) {
                                    FileSystem.SYSTEM.read(item.filePath.toPath()) { readByteArray() }
                                }
                                DocPhoto(bytes, "${room.label} — confirmed")
                            }
                            RoomSection(room.label, photos, container.api.listFindings(room.id))
                        }
                        val doc = PdfDoc(
                            title = "Rental Evidence Report",
                            verifyCode = verifyCode,
                            verifyUrl = verifyUrl,
                            manifestSha = manifestSha,
                            sealedAt = sealedAt,
                            sections = sections,
                        )
                        status = "Rendering PDF…"
                        val qr = withContext(Dispatchers.Default) { container.platformPdf.qr(verifyUrl) }
                        val bytes = withContext(Dispatchers.Default) {
                            container.platformPdf.render(doc, SealBlock(verifyCode, verifyUrl, manifestSha, qr))
                        }
                        val localSha = bytes.toByteString().sha256().hex()
                        status = "Uploading final PDF…"
                        val rep = container.api.attachPdf(reportId, bytes)
                        if (rep.reportSha256 != localSha) {
                            error = "Server hash differs — not saved."
                            status = "Failed."
                        } else {
                            val path = "${container.filesDir}/report-$reportId.pdf"
                            FileSystem.SYSTEM.write(path.toPath()) { write(bytes) }
                            pdfPath = path
                            status = "Saved and verified ✓ ${bytes.size / 1024} KB"
                        }
                    } catch (e: Exception) {
                        error = e.message?.take(200)
                        status = "Failed."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && verifyUrl.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Generate and upload PDF")
        }
        Button(
            onClick = { pdfPath?.let { container.platformPdf.share(it, "application/pdf") } },
            enabled = pdfPath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Share PDF")
        }
        Button(
            onClick = {
                scope.launch {
                    try {
                        check = container.api.verify(verifyCode)
                    } catch (e: Exception) {
                        error = e.message?.take(160)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Check verification")
        }
        check?.let {
            Text("Authentic: ${it.authentic} · evidence: ${it.evidenceCount} · signature: ${it.signatureValid}")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
