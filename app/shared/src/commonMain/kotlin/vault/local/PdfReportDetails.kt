package vault.local

internal fun ReportDetails.drawReportDetails(layout: PdfLayoutBuilder) {
    val values = listOf(
        "Responsável declarado" to authorName,
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
    values.forEach { (title, content) ->
        layout.text(title, TextKind.HEADING)
        layout.text(content, TextKind.BODY)
    }
    layout.text(if (authorAcknowledged) "Autor confirmou a autoria e revisou os dados antes do lacre local." else
        "Sem confirmação de autoria pessoal registrada.", TextKind.SMALL)
    layout.text("Assinatura local identifica a chave desta instalação. Não certifica identidade civil, concordância das partes ou conformidade normativa.", TextKind.SMALL)
    layout.flush()
}
