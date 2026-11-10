package vault.local

internal fun PdfModel.drawReportDetails(layout: PdfLayoutBuilder) = with(layout) {
    heading("01 / IDENTIFICAÇÃO", "Imóvel, autoria e escopo",
        "Contexto do registro e limites da documentação apresentada.")
    spaceTo(205.0)
    rule(tone = PdfTone.PINE)
    gap(17.0)
    field("ENDEREÇO DO IMÓVEL", address)
    fields("MUNICÍPIO / UF" to municipality, "REFERÊNCIA DO CONTRATO" to details.context.contractReference)
    field("IDENTIFICAÇÃO DA VISTORIA", inspectionId)

    gap(10.0)
    ensure(75.0)
    rule()
    gap(15.0)
    text("Autoria declarada", TextKind.HEADING)
    val author = listOf(details.authorName, details.authorTitle, details.authorRole)
        .filter { it.isNotBlank() }.joinToString("\n")
    fields("RESPONSÁVEL DECLARADO" to author, "PARTICIPANTES / CONTRATANTE" to details.participants)
    field("ORGANIZAÇÃO", details.context.organization)
    details.identification?.let {
        if (it.cpf.isNotBlank()) field("CPF DECLARADO", it.displayedCpf())
        fields("E-MAIL DECLARADO" to it.email, "TELEFONE DECLARADO" to it.phone)
    }
    text(if (details.authorAcknowledged) "Autor confirmou a autoria e revisou os dados antes do lacre local."
        else "Sem confirmação de autoria pessoal registrada.", TextKind.SMALL)
    if (details.profile == "technical") details.drawProfessionalIdentity(this)

    gap(14.0)
    ensure(65.0)
    text("Finalidade e método", TextKind.HEADING)
    text("Registro visual organizado por ambientes, com observações vinculadas às imagens.")
    field("FINALIDADE DECLARADA", details.purpose)
    field("OBJETO E ESCOPO", details.scope)
    field("MÉTODOS E PROCEDIMENTOS", details.methods)
    field("DOCUMENTOS EXAMINADOS", details.context.documentsReviewed)
    gap(10.0)
    panel("LIMITES DO REGISTRO", details.limitations.ifBlank {
        "O relatório documenta condições aparentes. A cobertura indica as categorias fotografadas."
    }, PdfTone.PAPER, accent = false)
    methodology.forEach { text(it, TextKind.SMALL, 2.0) }
    field("RESUMO", details.context.summary)
    field("PALAVRAS-CHAVE", details.context.keywords)
    flush()
}

private fun ReportDetails.drawProfessionalIdentity(layout: PdfLayoutBuilder) = with(layout) {
    fields("REGISTRO DECLARADO" to "$council/$councilState · $registration",
        "RESPONSABILIDADE DECLARADA" to "$responsibilityType · $responsibilityReference")
    val reviewed = professionalReview.attributionsAcknowledged && professionalReview.registeredResponsibilityAcknowledged &&
        professionalReview.standardsApplicabilityAcknowledged
    text(if (reviewed)
        "Atribuições, registro da responsabilidade e referências foram reconhecidos pelo autor. O aplicativo não consultou conselho ou certificou conformidade ABNT."
        else "Sem confirmações específicas de atribuições, registro e aplicabilidade neste manifesto histórico. O aplicativo não consultou conselho ou certificou conformidade ABNT.",
        TextKind.SMALL)
    if (professionalReview.consultationSource.isNotBlank()) {
        field("CONSULTA DECLARADA", "${professionalReview.consultationSource} · ${professionalReview.consultationDate}")
    }
    responsibilityDocument?.let {
        field("DOCUMENTO DE RESPONSABILIDADE ANEXADO", it.name)
        text("SHA-256: ${it.sha256}", TextKind.SMALL)
        text("O original acompanha o ZIP. Assinatura externa, registro e quitação não foram verificados pelo aplicativo.", TextKind.SMALL)
    }
}

internal fun ReportDetails.drawReportConclusions(layout: PdfLayoutBuilder) {
    val sections = listOf("ANÁLISE" to analysis, "CONCLUSÕES" to conclusions, "RECOMENDAÇÕES" to recommendations,
        "REFERÊNCIAS" to context.references, "NORMAS E REFERÊNCIAS DECLARADAS" to standards)
        .filter { it.second.isNotBlank() }
    if (sections.isEmpty()) return
    layout.heading("03 / DECLARAÇÕES", "Análise e conclusões")
    sections.forEach { (title, content) -> layout.field(title, content) }
    layout.flush()
}

private fun PdfLayoutBuilder.field(label: String, value: String) {
    if (value.isBlank()) return
    ensure(40.0)
    text(label, TextKind.LABEL, 5.0)
    text(value, gapAfter = 15.0)
}

private fun PdfLayoutBuilder.fields(left: Pair<String, String>, right: Pair<String, String>) {
    val first = PdfColumn(pdfBody.x, 217.0)
    val second = PdfColumn(322.0, pdfRight - 322)
    val leftLines = lines(left.second, TextKind.BODY, first)
    val rightLines = lines(right.second, TextKind.BODY, second)
    val lineCount = maxOf(leftLines.size, rightLines.size)
    if (left.second.isBlank() || right.second.isBlank() || lineCount > 4) {
        field(left.first, left.second)
        field(right.first, right.second)
        return
    }
    val block = 19.0 + height(TextKind.BODY) * lineCount + 15
    ensure(block)
    put(left.first, TextKind.LABEL, y, first, PdfTone.MUTED)
    put(right.first, TextKind.LABEL, y, second, PdfTone.MUTED)
    cell(left.second, TextKind.BODY, y + 19, first)
    cell(right.second, TextKind.BODY, y + 19, second)
    gap(block)
}
