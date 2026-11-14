package vault.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import vault.account.*

@Composable
fun AccountScreen(factory: ((String) -> AccountSessions)?) {
    var origin by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<AccountSessions?>(null) }
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<AccountProfile?>(null) }
    var hasSaved by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<DeviceSession>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var canRecover by remember { mutableStateOf(false) }
    var confirmRecovery by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DisposableEffect(session) { val selected = session; onDispose { selected?.close() } }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; message = null
        scope.launch {
            try { action(); canRecover = false } catch (e: CancellationException) { throw e
            } catch (e: SessionFailure) {
                if (e.code == "authentication_required" || e.code == "sign_in_required") { profile = null; devices = null }
                if (e.code == "authentication_required") hasSaved = false
                canRecover = e.code == "storage_unavailable" && session != null
                error = accountError(e.code)
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sua conta", style = MaterialTheme.typography.headlineSmall)
        Text("Conecte-se ao servidor para gerenciar sua conta. Suas vistorias locais continuam disponíveis.")
        if (session == null) {
            OutlinedTextField(origin, { origin = it }, label = { Text("Endereço HTTPS do servidor") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                run {
                    val candidate = factory?.invoke(origin.trim()) ?: throw SessionFailure("service_unavailable")
                    try { candidate.validateOrigin() }
                    catch (e: Exception) { candidate.close(); throw e }
                    session = candidate
                    hasSaved = candidate.hasSavedSession()
                }
            }, enabled = !busy && factory != null, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
        } else if (profile == null) {
            val selected = session!!
            TextButton(onClick = { session = null; password = ""; proof = "" }, enabled = !busy) { Text("Alterar servidor") }
            AccountForms(mode, email, password, confirmation, proof, busy, hasSaved,
                onMode = { mode = it; password = ""; confirmation = ""; proof = ""; error = null; message = null },
                onEmail = { email = it }, onPassword = { password = it },
                onConfirmation = { confirmation = it }, onProof = { proof = it },
                onSubmit = {
                    run {
                        when (mode) {
                            "login" -> { selected.login(email.trim(), password); hasSaved = true; password = ""; profile = selected.profile(); devices = selected.devices() }
                            "register" -> { selected.enrollment().requestVerification(email.trim()); message = "Se o endereço puder receber o cadastro, enviaremos as instruções." }
                            "verify" -> { selected.enrollment().verify(accountProof(proof, "verificar"), password); password = ""; proof = ""; message = "E-mail verificado. Entre com sua senha."; mode = "login" }
                            "recovery" -> { selected.enrollment().requestRecovery(email.trim()); message = "Se a conta puder ser recuperada, enviaremos as instruções." }
                            "reset" -> { selected.enrollment().resetPassword(accountProof(proof, "recuperar"), password, confirmation); password = ""; confirmation = ""; proof = ""; message = "Senha alterada. Entre novamente."; mode = "login" }
                        }
                    }
                }, onResume = { run { profile = selected.profile(); devices = selected.devices() } },
                onForget = { run { val result = selected.logout(); hasSaved = false; message = if (result.remoteRevoked) "Você saiu da conta." else "Conta removida deste dispositivo. Não foi possível confirmar a saída no servidor." } })
        } else {
            val selected = session!!
            AccountDevices(profile!!, devices, busy,
                onRefresh = { run { devices = null; profile = selected.profile(); devices = selected.devices() } },
                onRevoke = { id -> run { selected.revokeDevice(id); if (selected.currentUser.value == null) { profile = null; devices = null; hasSaved = false } else { devices = null; devices = selected.devices() } } },
                onLogout = { run { val result = selected.logout(); profile = null; devices = null; hasSaved = false; message = if (result.remoteRevoked) "Você saiu da conta." else "Conta removida deste dispositivo. Não foi possível confirmar a saída no servidor." } })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (canRecover) TextButton(onClick = { confirmRecovery = true }, enabled = !busy) { Text("Apagar credenciais locais") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Aguarde…") }
    }
    if (confirmRecovery) AlertDialog(onDismissRequest = { confirmRecovery = false },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Apagar credenciais locais?") },
        text = { Text("Você precisará entrar novamente. A saída no servidor não poderá ser confirmada. Suas vistorias locais serão mantidas.") },
        confirmButton = { TextButton(onClick = { confirmRecovery = false; run { session?.forget(); hasSaved = false; canRecover = false; message = "Credenciais locais apagadas. A saída no servidor não foi confirmada." } }) { Text("Apagar") } },
        dismissButton = { TextButton(onClick = { confirmRecovery = false }) { Text("Cancelar") } })
}

private fun accountError(code: String): String = when (code) {
    "insecure_origin" -> "Informe um endereço HTTPS válido, sem caminho ou parâmetros."
    "origin_mismatch" -> "A conta salva pertence a outro servidor. Informe o servidor original."
    "authentication_required", "sign_in_required" -> "Não foi possível validar a conta. Confira os dados ou entre novamente."
    "already_connected" -> "Já existe uma conta salva. Use Continuar sessão ou saia da conta."
    "invalid_request" -> "Confira os dados informados."
    "too_many_attempts" -> "Muitas tentativas. Aguarde antes de tentar novamente."
    "storage_locked" -> "Desbloqueie o dispositivo para acessar sua conta."
    "storage_unavailable" -> "Não foi possível acessar o armazenamento seguro."
    "session_limit" -> "Limite de dispositivos atingido. Remova uma sessão existente."
    else -> "Não foi possível concluir. Verifique a conexão e tente novamente."
}
