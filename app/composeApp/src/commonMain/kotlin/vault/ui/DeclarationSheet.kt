package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeclarationSheet(session: LocalSession, reportId: String, picker: LocalFilePicker?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val certificateSigner = remember { personalCertificateSigner() }
    var intent by remember { mutableStateOf(DeclarationIntent.AUTHOR) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var acknowledged by remember { mutableStateOf(false) }
    var useCertificate by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<PickedLocalFile?>(null) }
    var password by remember { mutableStateOf("") }
    var identity by remember { mutableStateOf<CertificateIdentity?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun clearCredential() {
        picked?.let { FileSystem.SYSTEM.delete(it.path.toPath(), mustExist = false) }
        picked = null; password = ""; identity = null
    }
    DisposableEffect(Unit) { onDispose { clearCredential() } }
    ModalBottomSheet(onDismissRequest = { if (!busy) onClose() }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Registrar declaração sobre este relatório", style = MaterialTheme.typography.titleLarge)
            Text("Escolha o que você declara. Abrir ou compartilhar não registra recebimento nem concordância.")
            DeclarationIntent.entries.forEach { choice ->
                TextButton(enabled = !busy, onClick = { intent = choice; acknowledged = false }) {
                    Text("${if (choice == intent) "● " else "○ "}${declarationLabel(choice)}")
                }
            }
            VaultTextField(name, { name = it.take(200) }, "Nome declarado (opcional)")
            VaultTextField(role, { role = it.take(100) }, "Papel / relação com o imóvel (opcional)")
            VaultTextField(note, { note = it.take(10000) }, if (intent == DeclarationIntent.DISAGREED) "Descreva a discordância (obrigatório)" else "Observação (opcional)")
            if (certificateSigner != null && picker != null) {
                Row { Checkbox(useCertificate, { if (!busy) { useCertificate = it; acknowledged = false; clearCredential() } }, enabled = !busy)
                    Text("Assinar com meu certificado A1 (.p12 / .pfx)") }
                if (useCertificate) {
                    Text("O certificado assina uma declaração vinculada ao PDF. A assinatura separada acompanha o ZIP; este formato não é PAdES.", style = MaterialTheme.typography.bodySmall)
                    VaultButton(picked?.name ?: "Selecionar certificado A1", {
                        busy = true; error = null
                        scope.launch {
                            try { clearCredential(); picked = picker.pick("*/*", 1_048_576) }
                            catch (e: Exception) { error = userErrorMessage(e) }
                            finally { busy = false }
                        }
                    }, enabled = !busy)
                    OutlinedTextField(password, { password = it; identity = null; acknowledged = false }, label = { Text("Senha do certificado") },
                        visualTransformation = PasswordVisualTransformation(), enabled = !busy, singleLine = true)
                    VaultButton("Conferir certificado", {
                        busy = true; error = null
                        scope.launch {
                            try { identity = withContext(Dispatchers.Default) { certificateSigner.inspect(requireNotNull(picked).path, password.toCharArray()) } }
                            catch (e: Exception) { error = userErrorMessage(e); identity = null }
                            finally { busy = false }
                        }
                    }, enabled = !busy && picked != null)
                    identity?.let {
                        Text("Titular do certificado: ${it.subject}\nEmissor: ${it.issuer}\nImpressão digital: ${it.fingerprint}", style = MaterialTheme.typography.bodySmall)
                        Text("Período declarado no certificado: ${it.validFrom} a ${it.validUntil}. Validade na assinatura conferida pelo relógio do dispositivo. Confiança externa, revogação e hora certificada não verificadas.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(if (useCertificate) "Sua chave privada e senha não integram o ZIP. Confira o titular acima antes de assinar." else
                "Registro atribuído à chave desta instalação. Nome e papel são declarados; este registro não comprova a participação de outra pessoa.", style = MaterialTheme.typography.bodySmall)
            Row { Checkbox(acknowledged, { acknowledged = it }, enabled = !busy)
                Text("Confirmo minha intenção: ${declarationLabel(intent)}. Autorizo registrar esta declaração no pacote.") }
            ErrorText(error)
            VaultButton(if (busy) "Registrando…" else "Registrar declaração", {
                busy = true; error = null
                scope.launch {
                    try {
                        withContext(Dispatchers.Default) {
                            ReportDeclarations(session.db, session.filesDir, platformSigner(session.filesDir)).append(
                                reportId, intent, name, role, note, acknowledged,
                                if (useCertificate) requireNotNull(picked).path else null, password.toCharArray())
                        }
                        clearCredential(); onClose()
                    } catch (e: Exception) { error = userErrorMessage(e) }
                    finally { password = ""; identity = null; busy = false }
                }
            }, enabled = !busy && acknowledged && (intent != DeclarationIntent.DISAGREED || note.isNotBlank()) && (!useCertificate || identity != null))
            TextButton(enabled = !busy, onClick = onClose) { Text("Fechar") }
        }
    }
}
