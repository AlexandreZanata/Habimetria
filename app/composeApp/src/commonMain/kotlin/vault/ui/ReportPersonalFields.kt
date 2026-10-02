package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import vault.local.*

@Composable
internal fun ReportPersonalFields(d: ReportDetails, enabled: Boolean, onChange: (ReportDetails) -> Unit) {
    val technical = d.profile == "technical"
    val identified = d.identification != null || d.authorName.isNotBlank()
    Text("Identificação", style = MaterialTheme.typography.titleMedium)
    if (!technical) Row {
        Switch(identified, { checked -> onChange(if (checked) d.copy(identification = PersonalIdentification()) else
            d.copy(identification = null, authorName = "", authorRole = "", authorTitle = "")) }, enabled = enabled)
        Text("Quero incluir minha identificação", Modifier.weight(1f))
    }
    if (technical || identified) {
        OutlinedTextField(d.authorName, { onChange(d.copy(authorName = it)) }, enabled = enabled,
            label = { Text(if (technical) "Nome do responsável profissional" else "Nome (opcional)") }, modifier = Modifier.fillMaxWidth())
        val identity = d.identification ?: PersonalIdentification()
        OutlinedTextField(identity.cpf, { onChange(d.copy(identification = identity.copy(cpf = it))) }, enabled = enabled,
            label = { Text("CPF (opcional; dados declarados)") }, modifier = Modifier.fillMaxWidth())
        if (identity.cpf.isNotBlank()) Row {
            Checkbox(identity.exposeCpf, { onChange(d.copy(identification = identity.copy(exposeCpf = it))) }, enabled = enabled)
            Text("Mostrar CPF completo no PDF", Modifier.weight(1f))
        }
        OutlinedTextField(identity.email, { onChange(d.copy(identification = identity.copy(email = it))) }, enabled = enabled,
            label = { Text("E-mail (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(identity.phone, { onChange(d.copy(identification = identity.copy(phone = it))) }, enabled = enabled,
            label = { Text("Telefone (opcional)") }, modifier = Modifier.fillMaxWidth())
        Text("A identificação é declarada. O ZIP verificável inclui os dados completos que você fornecer, mesmo quando o CPF estiver mascarado no PDF.",
            style = MaterialTheme.typography.bodySmall)
    } else Text("Você pode gerar o registro fotográfico sem nome, CPF ou contato.", style = MaterialTheme.typography.bodySmall)
}
