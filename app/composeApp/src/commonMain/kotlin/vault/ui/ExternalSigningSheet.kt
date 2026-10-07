package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.EXTERNAL_METHOD_GOVBR
import vault.local.EXTERNAL_METHOD_PADES
import vault.local.EXTERNAL_METHOD_TIMESTAMP
import vault.local.EXTERNAL_SIGNATURE_UNVERIFIED
import vault.local.LocalSession
import vault.local.ReportGenerator
import vault.local.externalInstructions
import vault.local.externalSignedPath
import vault.local.platformSigner

// Optional external signing: explicit user-driven handoff. Local READY
// stays offline and preserved; no PAdES/Gov.br/ACT validity is claimed
// here. Signed copies must be checked in their external verifier.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExternalSigningSheet(session: LocalSession, reportId: String, picker: LocalFilePicker?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf(EXTERNAL_METHOD_PADES) }
    var acknowledged by remember { mutableStateOf(false) }
    val generator = remember(session) {
        ReportGenerator(session.db, session.filesDir, session.originals, session.derivatives, platformSigner(session.filesDir))
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onClose() }) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Assinatura externa opcional (PAdES / Gov.br / carimbo do tempo)", style = MaterialTheme.typography.titleLarge)
            Text("O PDF local continua válido offline. A assinatura externa usa uma cópia: o original é preservado e a cópia nunca é marcada como verificada aqui.",
                style = MaterialTheme.typography.bodyMedium)
            Text("PAdES exige seu certificado A1 de titular autorizado em assinador externo compatível com ITI (RB/RT, DocMDP). Gov.br exige conta prata ou ouro no Assinador oficial; bronze não assina. Carimbo do tempo exige ACT contratada (RFC 3161). Nenhuma credencial Gov.br é digitada aqui; confira a cópia no ITI VALIDAR ou na autoridade emissora.",
                style = MaterialTheme.typography.bodySmall)
            listOf(EXTERNAL_METHOD_PADES, EXTERNAL_METHOD_GOVBR, EXTERNAL_METHOD_TIMESTAMP).forEach { choice ->
                androidx.compose.material3.TextButton(enabled = !busy, onClick = { method = choice }) {
                    Text("${if (choice == method) "● " else "○ "}$choice")
                }
            }
            listOf(EXTERNAL_METHOD_PADES, EXTERNAL_METHOD_GOVBR, EXTERNAL_METHOD_TIMESTAMP).forEach { method ->
                Text(method, style = MaterialTheme.typography.titleSmall)
                Text(externalInstructions(method), style = MaterialTheme.typography.bodySmall)
                val path = externalSignedPath(session.filesDir, reportId, method)
                if (FileSystem.SYSTEM.exists(path.toPath())) {
                    Text("Cópia registrada: $path · $EXTERNAL_SIGNATURE_UNVERIFIED · confira no verificador externo.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Para registrar uma cópia assinada externamente, selecione o arquivo no verificador local após assinar fora do app.",
                style = MaterialTheme.typography.bodySmall)
            androidx.compose.material3.TextButton(enabled = !busy, onClick = { acknowledged = !acknowledged }) {
                Text("${if (acknowledged) "● " else "○ "}Confirmo que assinei uma cópia fora do app com o titular/autoridade indicados e que a cópia continua sem verificação local")
            }
            if (picker != null) {
                VaultButton("Registrar cópia externa ($method)", {
                    busy = true
                    error = null
                    scope.launch {
                        var picked: PickedLocalFile? = null
                        try {
                            check(acknowledged) { "confirme a intenção explícita antes de registrar" }
                            val selected = picker.pick("application/pdf", 50L * 1024 * 1024)
                                ?: error("nenhum arquivo selecionado")
                            picked = selected
                            val selectedPath = selected.path
                            val bytes = withContext(Dispatchers.Default) {
                                FileSystem.SYSTEM.read(selectedPath.toPath()) { readByteArray() }
                            }
                            val report = generator.reportById(reportId) ?: error("report not found")
                            withContext(Dispatchers.Default) {
                                vault.local.registerExternalSignedCopy(session.filesDir, report, method, bytes, acknowledged)
                            }
                            FileSystem.SYSTEM.delete(selectedPath.toPath(), mustExist = false)
                            onClose()
                        } catch (e: Exception) {
                            picked?.let { runCatching { FileSystem.SYSTEM.delete(it.path.toPath(), mustExist = false) } }
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                }, enabled = !busy && acknowledged)
            }
            ErrorText(error)
            VaultButton("Fechar", onClose, enabled = !busy)
            if (busy) Text("Aguarde…", style = MaterialTheme.typography.bodySmall)
        }
    }
}
