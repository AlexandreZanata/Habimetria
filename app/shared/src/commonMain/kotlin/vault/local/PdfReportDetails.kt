package vault.local

internal fun ReportDetails.drawReportDetails(layout: PdfLayoutBuilder) {
    val values = listOf(
        "Responsável declarado" to authorName,
        "Título / papel declarado" to listOf(authorTitle, authorRole).filter { it.isNotBlank() }.joinToString(" · "),
        "Município / UF do imóvel" to listOf(propertyMunicipality, propertyState).filter { it.isNotBlank() }.joinToString(" / "),
        "Participantes / contratante" to participants,
        "Finalidade" to purpose,
        "Objeto e escopo" to scope,
        "Métodos e procedimentos" to methods,
        "Limitações e exclusões" to limitations,
        "Análise" to analysis,
        "Conclusões" to conclusions,
        "Recomendações" to recommendations,
        "Referências normativas declaradas" to standards,
    ).filter { it.second.isNotBlank() }
    if (values.isEmpty() && profile != "technical") return
    layout.text("Identificação e escopo", TextKind.HEADING)
    if (profile == "technical") {
        layout.text("Registro declarado: $council/$councilState · $registration", TextKind.BODY)
        layout.text("Responsabilidade declarada: $responsibilityType · $responsibilityReference", TextKind.BODY)
        layout.text("Referência fornecida pelo autor; registro, atribuições e ART/RRT não foram consultados pelo aplicativo.", TextKind.SMALL)
    }
    responsibilityDocument?.let { document ->
        layout.text("Documento de responsabilidade anexado: ${document.name}", TextKind.BODY)
        layout.text("SHA-256: ${document.sha256}", TextKind.SMALL)
        layout.text("O original acompanha o pacote ZIP. O aplicativo não validou assinatura externa, registro ou quitação do documento.", TextKind.SMALL)
    }
    values.forEach { (title, content) ->
        layout.text(title, TextKind.HEADING)
        layout.text(content, TextKind.BODY)
    }
    layout.text(if (authorAcknowledged) "Autor confirmou a autoria e revisou os dados antes do lacre local." else
        "Sem confirmação de autoria pessoal registrada.", TextKind.SMALL)
    layout.text("Assinatura local identifica a chave desta instalação. Não certifica identidade civil, concordância das partes ou conformidade normativa.", TextKind.SMALL)
    layout.flush()
}
