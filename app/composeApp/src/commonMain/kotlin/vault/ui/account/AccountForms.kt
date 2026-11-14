package vault.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AccountForms(mode: String, email: String, password: String, confirmation: String, proof: String,
    busy: Boolean, hasSaved: Boolean, onMode: (String) -> Unit, onEmail: (String) -> Unit, onPassword: (String) -> Unit,
    onConfirmation: (String) -> Unit, onProof: (String) -> Unit, onSubmit: () -> Unit, onResume: () -> Unit,
    onForget: () -> Unit) {
    Text(when (mode) { "register" -> "Criar conta"; "verify" -> "Verificar e-mail"; "recovery" -> "Recuperar senha"; "reset" -> "Definir nova senha"; else -> "Entrar" }, style = MaterialTheme.typography.titleLarge)
    if (mode in listOf("login", "register", "recovery"))
        OutlinedTextField(email, onEmail, label = { Text("E-mail") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
    if (mode in listOf("verify", "reset"))
        OutlinedTextField(proof, onProof, label = { Text("Link ou código recebido por e-mail") }, modifier = Modifier.fillMaxWidth())
    if (mode in listOf("login", "verify", "reset"))
        OutlinedTextField(password, onPassword, label = { Text("Senha") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    if (mode in listOf("verify", "reset")) Text("Escolha uma senha com pelo menos 15 caracteres.", style = MaterialTheme.typography.bodySmall)
    if (mode == "reset") OutlinedTextField(confirmation, onConfirmation, label = { Text("Confirmar senha") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = onSubmit, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(when (mode) { "register", "recovery" -> "Enviar instruções"; "verify" -> "Verificar"; "reset" -> "Alterar senha"; else -> "Entrar" }) }
    if (mode == "login") {
        if (hasSaved) {
            OutlinedButton(onClick = onResume, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Continuar sessão salva") }
            TextButton(onClick = onForget, enabled = !busy) { Text("Sair da conta neste dispositivo") }
        }
        TextButton(onClick = { onMode("register") }, enabled = !busy) { Text("Criar conta") }
        TextButton(onClick = { onMode("recovery") }, enabled = !busy) { Text("Esqueci minha senha") }
    } else {
        if (mode == "register") TextButton(onClick = { onMode("verify") }, enabled = !busy) { Text("Já recebi o código de verificação") }
        if (mode == "recovery") TextButton(onClick = { onMode("reset") }, enabled = !busy) { Text("Já recebi o código de recuperação") }
        TextButton(onClick = { onMode("login") }, enabled = !busy) { Text("Voltar para entrar") }
    }
}
