package vault.local

internal fun reportCover(model: PdfModel, renderer: PdfTextMetrics): List<RenderPage> {
    val cover = PdfLayoutBuilder(renderer, model.reportId)
    cover.text(if (model.details.profile == "technical") "RELATÓRIO TÉCNICO DE VISTORIA" else "RELATÓRIO DE VISTORIA", TextKind.TITLE, 14.0)
    cover.text("Registro fotográfico do imóvel", TextKind.HEADING)
    if (model.details.context.organization.isNotBlank()) cover.text(model.details.context.organization)
    if (model.details.context.inspectionDate.isNotBlank()) cover.text("Data da vistoria declarada: ${model.details.context.inspectionDate}")
    cover.text(model.address)
    val municipality = listOf(model.details.propertyMunicipality, model.details.propertyState).filter { it.isNotBlank() }.joinToString(" / ")
    if (municipality.isNotBlank()) cover.text(municipality)
    if (model.details.authorName.isNotBlank()) cover.text("Responsável declarado: ${model.details.authorName}")
    val role = listOf(model.details.authorTitle, model.details.authorRole).filter { it.isNotBlank() }.joinToString(" · ")
    if (role.isNotBlank()) cover.text(role, TextKind.SMALL)
    if (model.details.profile == "technical") cover.text("Registro declarado: ${model.details.council}/${model.details.councilState} · ${model.details.registration}", TextKind.SMALL)
    if (model.details.purpose.isNotBlank()) cover.text("Finalidade: ${model.details.purpose}")
    cover.text("Vistoria: ${model.inspectionId}", TextKind.SMALL)
    cover.text("Lacre informado pelo dispositivo: ${model.sealedAtDevice}")
    val photos = model.rooms.sumOf { it.photos.size }
    val findings = model.rooms.sumOf { it.findings.size }
    cover.text("${model.rooms.size} ${if (model.rooms.size == 1) "ambiente" else "ambientes"} · " +
        "$photos ${if (photos == 1) "fotografia" else "fotografias"} · " +
        "$findings ${if (findings == 1) "observação" else "observações"}")
    cover.gap(20.0)
    cover.qr(qrContent(model.reportId, model.manifestSha256))
    cover.text("O QR identifica o relatório. Para verificar seus bytes, utilize o PDF e o recibo assinado.", TextKind.SMALL)
    cover.text("Hora e localização são informações do dispositivo. Compartilhamento não significa concordância. Declarações posteriores acompanham o ZIP.", TextKind.SMALL)
    cover.flush()
    if (model.details.context.summary.isNotBlank()) {
        cover.text("RESUMO", TextKind.HEADING)
        cover.text(model.details.context.summary)
        if (model.details.context.keywords.isNotBlank()) cover.text("Palavras-chave: ${model.details.context.keywords}")
        cover.flush()
    }
    return cover.pages

}
