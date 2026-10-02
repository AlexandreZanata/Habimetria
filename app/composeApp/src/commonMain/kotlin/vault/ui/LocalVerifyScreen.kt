package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.*
import vault.local.db.TrustedIssuer

@Composable
fun LocalVerifyScreen(session: LocalSession, inspectionId: String, picker: LocalFilePicker?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val signer = remember(session) { platformSigner(session.filesDir) }
    val generator = remember(session) { ReportGenerator(session.db, session.filesDir, session.originals, session.derivatives, signer) }
    var qrText by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<PackageVerdict?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyLabel by remember { mutableStateOf("") }
    var importedKey by remember { mutableStateOf<String?>(null) }
    var trusted by remember { mutableStateOf<List<TrustedIssuer>>(emptyList()) }
    LaunchedEffect(session) { trusted = session.trust.list() }

    suspend fun checkPackage(directory: String) {
        val pinned = session.trust.trustedKeys()
        val certified = session.trust.certifiedKeys()
        val checked = withContext(Dispatchers.Default) { PackageVerifier(signer).verify(directory, pinned, certified) }
        verdict = checked; importedKey = checked.signerKeyId
    }
    fun checkLocal(reportId: String?) {
        busy = true; error = null; verdict = null
        scope.launch {
            try {
                val id = reportId ?: generator.manifestFor(inspectionId)?.let { generator.existingReport(it.id)?.id }
                    ?: throw LocalValidationException("report not found")
                val directory = withContext(Dispatchers.Default) { generator.exportPackage(id) }
                documentName = "Relatório neste dispositivo"
                checkPackage(directory)
            } catch (e: Exception) { error = userErrorMessage(e) }
            finally { busy = false }
        }
    }
    ScreenColumn {
        ScreenTitle(Strings.verifyTitle)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Verificação de arquivos e assinaturas locais. Não confirma a verdade das fotos nem a identidade civil do autor.", style = MaterialTheme.typography.bodyMedium)
            ErrorText(error)
            VaultButton("Verificar relatório deste dispositivo", { checkLocal(null) }, enabled = !busy)
            if (picker != null) VaultButton("Selecionar pacote ZIP recebido", {
                busy = true; error = null; verdict = null
                scope.launch {
                    try {
                        val selected = picker.pick("application/zip")
                        if (selected != null) {
                            val directory = try { withContext(Dispatchers.Default) { importPackageZip(selected.path, session.filesDir) } }
                                finally { FileSystem.SYSTEM.delete(selected.path.toPath(), mustExist = false) }
                            try { documentName = selected.name; checkPackage(directory) }
                            finally { withContext(Dispatchers.Default) { FileSystem.SYSTEM.deleteRecursively(directory.toPath()) } }
                        }
                    } catch (e: Exception) { error = userErrorMessage(e) }
                    finally { busy = false }
                }
            }, enabled = !busy)
            Text("Use o ZIP original exportado pelo app. ZIP recomprimido por outros programas não é suportado.", style = MaterialTheme.typography.bodySmall)
            documentName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            verdict?.let { VerdictBlock(it) }
            verdict?.declarations?.distinctBy { it.keyId }?.filter { it.keyId != importedKey }?.forEach { declaration ->
                Text("Compare por outro meio a impressão digital do declarante: ${declaration.keyId}", style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !busy && declaration.valid && !declaration.trusted, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            session.trust.trust(declaration.keyId, declaration.actorName.ifBlank { declaration.keyId.take(12) })
                            trusted = session.trust.list()
                            verdict = verdict?.copy(declarations = verdict!!.declarations.map {
                                if (it.keyId == declaration.keyId) it.copy(trusted = true) else it
                            })
                        } catch (e: Exception) { error = userErrorMessage(e) }
                        finally { busy = false }
                    }
                }) { Text("Confio neste declarante após comparar a impressão") }
            }
            importedKey?.let { fingerprint ->
                Text("Impressão digital da chave: $fingerprint", style = MaterialTheme.typography.bodySmall)
                VaultTextField(keyLabel, { keyLabel = it }, "Nome para esta chave")
                Text("Só marque confiança após comparar esta impressão digital por outro meio. Isso não transforma a chave em certificado de identidade.", style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !busy && verdict?.signatureValid == true, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            session.trust.trust(fingerprint, keyLabel.ifBlank { fingerprint.take(12) })
                            trusted = session.trust.list()
                            verdict = verdict?.copy(issuerTrust = IssuerTrust.PINNED, declarations = verdict!!.declarations.map { if (it.keyId == fingerprint) it.copy(trusted = true) else it })
                        } catch (e: Exception) { error = userErrorMessage(e) }
                        finally { busy = false }
                    }
                }) { Text("Confio nesta chave após comparar a impressão") }
            }
            trusted.forEach { issuer ->
                Text("Chave conhecida: ${issuer.label} · ${issuer.keyId}", style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            session.trust.revoke(issuer.keyId)
                            trusted = session.trust.list()
                            verdict = verdict?.copy(declarations = verdict!!.declarations.map { if (it.keyId == issuer.keyId) it.copy(trusted = false) else it })
                            if (verdict?.signerKeyId == issuer.keyId) verdict = verdict?.copy(issuerTrust = IssuerTrust.UNKNOWN)
                        } catch (e: Exception) { error = userErrorMessage(e) }
                        finally { busy = false }
                    }
                }) { Text("Remover confiança nesta chave") }
            }
            Text("Identificador do QR", style = MaterialTheme.typography.titleMedium)
            VaultTextField(qrText, { qrText = it }, Strings.qrLabel)
            VaultButton("Localizar e verificar pelo identificador", {
                val ref = parseQrIdentity(qrText)
                if (ref == null) error = Strings.qrBad else scope.launch {
                    try {
                        val row = generator.reportById(ref.reportId)
                        if (row?.manifestSha256 != ref.manifestSha256) error = Strings.qrNotFound else checkLocal(row.id)
                    } catch (e: Exception) { error = userErrorMessage(e) }
                }
            }, enabled = !busy)
            Text("O QR é um identificador; não é prova de integridade sozinho.", style = MaterialTheme.typography.bodySmall)
        }
        VaultButton(Strings.back, onBack, enabled = !busy)
    }
}
