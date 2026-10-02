package vault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import vault.local.DeclarationCheck
import vault.local.DeclarationIntent

internal fun declarationLabel(intent: DeclarationIntent): String = when (intent) {
    DeclarationIntent.AUTHOR -> "Declaro autoria"
    DeclarationIntent.RECEIVED -> "Declaro recebimento"
    DeclarationIntent.AGREED -> "Declaro concordância"
    DeclarationIntent.DISAGREED -> "Declaro discordância"
}

@Composable
internal fun DeclarationStatus(item: DeclarationCheck) {
    Text("${declarationLabel(item.intent)} · ${item.actorName.ifBlank { "Sem nome declarado" }}", style = MaterialTheme.typography.titleSmall)
    if (item.actorRole.isNotBlank()) Text(item.actorRole)
    if (item.note.isNotBlank()) Text(item.note)
    Text("Assinatura: ${if (item.valid) "válida" else "inválida"} · ${item.createdAtDevice} (dispositivo)", style = MaterialTheme.typography.bodySmall)
    item.certificate?.let { Text("Titular do certificado: ${it.subject}", style = MaterialTheme.typography.bodySmall) }
    Text("Chave / certificado: ${item.keyId}", style = MaterialTheme.typography.bodySmall)
    Text("Confiança: ${if (item.trusted) "impressão comparada e marcada pelo usuário" else "não verificada"}. Revogação e hora certificada não verificadas.", style = MaterialTheme.typography.bodySmall)
}
