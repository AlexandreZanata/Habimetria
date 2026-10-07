package vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import vault.local.*
import okio.Path.Companion.toPath

@Composable
fun ReportDetailsSheet(session: LocalSession, inspectionId: String, readOnly: Boolean, picker: LocalFilePicker?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember(session) { ReportDetailsRepository(session.db) }
    var details by remember { mutableStateOf<ReportDetails?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(inspectionId) {
        try {
            details = if (readOnly) repository.frozen(inspectionId) else repository.get(inspectionId)
        } catch (e: Exception) { error = userErrorMessage(e) }
    }
    Dialog(onDismissRequest = { if (!busy) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dados do relatório", style = MaterialTheme.typography.titleLarge)
                ErrorText(error)
                details?.let { d ->
                    Text(if (d.profile == "technical") "Vistoria profissional" else "Vistoria de locação")
                    Text("Estes dados serão congelados no lacre. A captura continua sendo foto + problema + nota opcional.", style = MaterialTheme.typography.bodySmall)
                    ReportDetailFields(d, enabled = !readOnly && !busy, onChange = { details = it })
                    d.responsibilityDocument?.let { Text("Documento anexado: ${it.name}", style = MaterialTheme.typography.bodySmall) }
                    if (d.profile == "technical" && !readOnly && picker != null) TextButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try {
                                repository.save(inspectionId, d)
                                val selected = picker.pick("application/pdf", 10L * 1024 * 1024)
                                if (selected != null) {
                                    try { details = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                        ResponsibilityDocuments(session.filesDir).attach(session.db, inspectionId, selected.path, selected.name)
                                    } } finally { okio.FileSystem.SYSTEM.delete(selected.path.toPath(), mustExist = false) }
                                }
                            } catch (e: Exception) { error = userErrorMessage(e) }
                            finally { busy = false }
                        }
                    }) { Text("Anexar PDF da ART/RRT (obrigatório)") }
                    Row {
                        Checkbox(checked = d.authorAcknowledged, onCheckedChange = { details = d.copy(authorAcknowledged = it) }, enabled = !readOnly && !busy)
                        Text("Confirmo minha autoria e a revisão dos dados. No uso profissional, confirmo minhas atribuições e a responsabilidade indicada.", modifier = Modifier.weight(1f))
                    }
                    Text("O aplicativo registra declarações e integridade dos arquivos. Não consulta registro profissional nem certifica identidade ou aceitação jurídica.", style = MaterialTheme.typography.bodySmall)
                    if (!readOnly) VaultButton(text = "Salvar dados", enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try { repository.save(inspectionId, d); onClose() }
                            catch (e: Exception) { error = userErrorMessage(e) }
                            finally { busy = false }
                        }
                    })
                }
                TextButton(onClick = onClose, enabled = !busy) { Text(Strings.back) }
            }
        }
    }
}

@Composable
private fun ReportDetailFields(d: ReportDetails, enabled: Boolean, onChange: (ReportDetails) -> Unit) {
    @Composable fun field(label: String, value: String, update: (String) -> ReportDetails) {
        OutlinedTextField(value, { onChange(update(it)) }, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth())
    }
    ReportPersonalFields(d, enabled, onChange)
    field("Papel do responsável (ex.: locatário, proprietário)", d.authorRole) { d.copy(authorRole = it) }
    field("Município do imóvel", d.propertyMunicipality) { d.copy(propertyMunicipality = it) }
    field("UF do imóvel", d.propertyState) { d.copy(propertyState = it.uppercase()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("standard" to "Padrão", "detail" to "Fotos ampliadas").forEach { (value, label) ->
            FilterChip(selected = d.photoLayout == value, enabled = enabled, onClick = { onChange(d.copy(photoLayout = value)) }, label = { Text(label) })
        }
    }
    field("Participantes / contratante", d.participants) { d.copy(participants = it) }
    field("Finalidade", d.purpose) { d.copy(purpose = it) }
    field("Objeto e escopo", d.scope) { d.copy(scope = it) }
    if (d.profile == "technical") {
        field("Título profissional declarado", d.authorTitle) { d.copy(authorTitle = it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CREA", "CAU").forEach { council ->
                FilterChip(selected = d.council == council, enabled = enabled,
                    onClick = { onChange(d.copy(council = council, responsibilityType = if (council == "CREA") "ART" else "RRT")) }, label = { Text(council) })
            }
        }
        field("UF do conselho", d.councilState) { d.copy(councilState = it.uppercase()) }
        field("Número do registro", d.registration) { d.copy(registration = it) }
        field("Número / referência de ${d.responsibilityType.ifBlank { "ART/RRT" }}", d.responsibilityReference) { d.copy(responsibilityReference = it) }
        field("Métodos e procedimentos", d.methods) { d.copy(methods = it) }
        field("Limitações e exclusões", d.limitations) { d.copy(limitations = it) }
        field("Análise", d.analysis) { d.copy(analysis = it) }
        field("Conclusões", d.conclusions) { d.copy(conclusions = it) }
        field("Recomendações", d.recommendations) { d.copy(recommendations = it) }
        field("Referências normativas usadas", d.standards) { d.copy(standards = it) }
    }
    ReportContextFields(d, enabled, onChange)
}
