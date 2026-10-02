package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import vault.local.*

@Composable
internal fun ReportContextFields(d: ReportDetails, enabled: Boolean, onChange: (ReportDetails) -> Unit) {
    val c = d.context
    @Composable fun field(label: String, value: String, update: (String) -> ReportContext) {
        OutlinedTextField(value, { onChange(d.copy(context = update(it))) }, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth())
    }
    Text("Contexto e apresentação", style = MaterialTheme.typography.titleMedium)
    if (d.profile == "technical") Text("Data da vistoria, resumo e referências são obrigatórios para o perfil profissional.", style = MaterialTheme.typography.bodySmall)
    field("Contrato / referência (opcional)", c.contractReference) { c.copy(contractReference = it) }
    field("Data declarada da vistoria (AAAA-MM-DD)", c.inspectionDate) { c.copy(inspectionDate = it) }
    field("Empresa / organização (opcional)", c.organization) { c.copy(organization = it) }
    field("Resumo", c.summary) { c.copy(summary = it) }
    field("Palavras-chave (separadas por ponto e vírgula)", c.keywords) { c.copy(keywords = it) }
    field("Documentos consultados", c.documentsReviewed) { c.copy(documentsReviewed = it) }
    field("Referências bibliográficas / técnicas", c.references) { c.copy(references = it) }
    if (d.profile == "technical") {
        val p = d.professionalReview
        @Composable fun confirmation(label: String, checked: Boolean, update: (Boolean) -> ProfessionalReview) {
            Row { Checkbox(checked, { onChange(d.copy(professionalReview = update(it))) }, enabled = enabled); Text(label, Modifier.weight(1f)) }
        }
        Text("Responsabilidade profissional", style = MaterialTheme.typography.titleMedium)
        confirmation("Declaro habilitação e atribuições para este serviço.", p.attributionsAcknowledged) { p.copy(attributionsAcknowledged = it) }
        confirmation("Declaro que a ART/RRT anexada está registrada e corresponde a este serviço.", p.registeredResponsibilityAcknowledged) { p.copy(registeredResponsibilityAcknowledged = it) }
        confirmation("Revisei as normas e exigências aplicáveis ao escopo e à localidade.", p.standardsApplicabilityAcknowledged) { p.copy(standardsApplicabilityAcknowledged = it) }
        OutlinedTextField(p.consultationSource, { onChange(d.copy(professionalReview = p.copy(consultationSource = it))) }, enabled = enabled,
            label = { Text("Fonte consultada pelo profissional (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(p.consultationDate, { onChange(d.copy(professionalReview = p.copy(consultationDate = it))) }, enabled = enabled,
            label = { Text("Data dessa consulta (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Text("Confirmações prestadas pelo responsável. O aplicativo não consultou conselho ou normas e não emite certificação ABNT.", style = MaterialTheme.typography.bodySmall)
    }
}
