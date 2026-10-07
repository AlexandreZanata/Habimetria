package vault.local

internal fun ReportDetails.drawReportDetails(layout: PdfLayoutBuilder) {
    layout.section("1 Identificação, objeto e procedimentos")
    val values = listOf(
        "Responsável declarado" to authorName,
        "Título / papel declarado" to listOf(authorTitle, authorRole).filter { it.isNotBlank() }.joinToString(" · "),
        "Município / UF do imóvel" to listOf(propertyMunicipality, propertyState).filter { it.isNotBlank() }.joinToString(" / "),
        "Data da vistoria declarada" to context.inspectionDate,
        "Referência do contrato" to context.contractReference,
        "Organização" to context.organization,
        "Participantes / contratante" to participants,
        "Finalidade" to purpose,
        "Objeto e escopo" to scope,
        "Métodos e procedimentos" to methods,
        "Documentos examinados" to context.documentsReviewed,
        "Limitações e exclusões" to limitations,
    )
    if (profile == "technical") {
        layout.text("Registro declarado: $council/$councilState · $registration")
        layout.text("Responsabilidade declarada: $responsibilityType · $responsibilityReference")
        layout.text(if (professionalReview.attributionsAcknowledged && professionalReview.registeredResponsibilityAcknowledged && professionalReview.standardsApplicabilityAcknowledged)
            "Atribuições, registro da responsabilidade e referências foram reconhecidos pelo autor. O aplicativo não consultou conselho ou certificou conformidade ABNT." else
            "Sem confirmações específicas de atribuições, registro e aplicabilidade neste manifesto histórico. O aplicativo não consultou conselho ou certificou conformidade ABNT.", TextKind.SMALL)
        if (professionalReview.consultationSource.isNotBlank()) layout.text("Consulta declarada pelo autor: ${professionalReview.consultationSource} · ${professionalReview.consultationDate}", TextKind.SMALL)
    }
    identification?.let {
        if (it.cpf.isNotBlank()) layout.text("CPF declarado: ${it.displayedCpf()}")
        if (it.email.isNotBlank()) layout.text("Contato declarado: ${it.email}")
        if (it.phone.isNotBlank()) layout.text("Telefone declarado: ${it.phone}")
    }
    responsibilityDocument?.let {
        layout.text("Documento de responsabilidade anexado: ${it.name}")
        layout.text("SHA-256: ${it.sha256}", TextKind.SMALL)
        layout.text("O original acompanha o ZIP. Assinatura externa, registro e quitação não foram verificados pelo aplicativo.", TextKind.SMALL)
    }
    values.filter { it.second.isNotBlank() }.forEach { (title, content) ->
        layout.text(title, TextKind.HEADING); layout.text(content)
    }
    layout.text(if (authorAcknowledged) "Autor confirmou a autoria e revisou os dados antes do lacre local." else
        "Sem confirmação de autoria pessoal registrada.", TextKind.SMALL)
    layout.text("Assinatura local identifica a instalação. Identidade civil e concordância das partes não são certificadas.", TextKind.SMALL)
    layout.flush()
}

internal fun ReportDetails.drawReportConclusions(layout: PdfLayoutBuilder) {
    listOf("3 Análise" to analysis, "4 Conclusões" to conclusions, "5 Recomendações" to recommendations,
        "6 Referências" to context.references, "Normas e referências declaradas" to standards)
        .filter { it.second.isNotBlank() }.forEach { (title, content) ->
            layout.section(title); layout.text(content)
        }
    layout.flush()
}
