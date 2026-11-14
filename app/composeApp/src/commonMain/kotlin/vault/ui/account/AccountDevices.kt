package vault.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import vault.account.AccountProfile
import vault.account.DeviceSession
import vault.ui.LoadingSkeleton
import vault.ui.SkeletonCard
import vault.ui.SkeletonLine
import vault.ui.rememberLoadingVisible

@Composable
fun AccountDevices(profile: AccountProfile, devices: List<DeviceSession>?, busy: Boolean,
    onRefresh: () -> Unit, onRevoke: (String) -> Unit, onLogout: () -> Unit) {
    var selected by remember { mutableStateOf<DeviceSession?>(null) }
    Text(profile.name.ifBlank { "Minha conta" }, style = MaterialTheme.typography.titleLarge)
    Text(profile.email)
    Text("Dispositivos conectados", style = MaterialTheme.typography.titleMedium)
    val loading = rememberLoadingVisible(busy && devices == null)
    if (loading) LoadingSkeleton {
        repeat(2) { SkeletonCard { SkeletonLine(0.65f); SkeletonLine(0.4f) } }
    }
    if (devices == null && !busy) Text("Atualize para carregar os dispositivos.")
    if (devices?.isEmpty() == true) Text("Nenhum dispositivo ativo encontrado.")
    devices?.forEach { device ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(device.label, style = MaterialTheme.typography.titleMedium)
                Text(if (device.platform == "android") "Android" else "iOS")
                Text("Último acesso: ${device.lastUsedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { selected = device }, enabled = !busy) { Text("Encerrar sessão") }
            }
        }
    }
    OutlinedButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Atualizar dispositivos") }
    Button(onClick = onLogout, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Sair desta conta") }
    selected?.let { device ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Encerrar sessão?") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = { Text("O dispositivo ${device.label} precisará entrar novamente. Os dados locais dele não serão apagados.") },
            confirmButton = { TextButton(onClick = { selected = null; onRevoke(device.id) }) { Text("Encerrar sessão") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancelar") } })
    }
}
