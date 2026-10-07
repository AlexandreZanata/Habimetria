package vault.local

private object DefaultPdfMetrics : PdfTextMetrics

fun paginateModel(model: PdfModel, dimsOf: (String) -> ImageDims?): List<RenderPage> =
    paginateModel(model, DefaultPdfMetrics, dimsOf)

fun paginateModel(model: PdfModel, renderer: PdfTextMetrics, dimsOf: (String) -> ImageDims?): List<RenderPage> {
    val cover = reportCover(model, renderer)

    val body = PdfLayoutBuilder(renderer, model.reportId, if (model.details.photoLayout == "detail") 520.0 else PDF_MAX_PHOTO_HEIGHT_PT)
    model.details.drawReportDetails(body)
    val roomStarts = mutableListOf<Pair<PdfRoomSection, Int>>()
    for (room in model.rooms) {
        body.flush()
        roomStarts.add(room to body.currentNumber)
        if (roomStarts.size == 1) body.section("2 Registro fotográfico por ambiente")
        body.section("2.${roomStarts.size} ${room.label}")
        body.text("Cobertura por categorias: ${room.coveragePercent}% · ${room.photos.size} fotos", TextKind.SMALL)
        if (room.missingCategories.isNotEmpty()) body.text("Categorias sem foto: ${room.missingCategories.joinToString(", ")}", TextKind.SMALL)
        if (room.notApplicableCategories.isNotEmpty()) body.text("Declaradas não aplicáveis: ${room.notApplicableCategories.joinToString(", ")}", TextKind.SMALL)
        for (photo in room.photos) {
            val dims = dimsOf(photo.evidenceId) ?: throw LocalValidationException("undecodable photo: ${photo.evidenceId}")
            body.photo(photo, dims)
        }
        if (room.findings.isNotEmpty()) body.text("Observações registradas", TextKind.HEADING)
        for (finding in room.findings) {
            body.marker(PageItem.Finding(finding))
            body.text("${finding.preset} · ${finding.evidenceRef}")
            finding.note?.let { body.text(it) }
        }
    }
    body.flush()
    model.details.drawReportConclusions(body)
    body.section("Apêndice A · Metodologia e limitações do registro")
    model.methodology.forEach { body.text(it) }
    body.flush()
    body.section("Apêndice B · Verificação de integridade")
    body.text("Relatório: ${model.reportId}", TextKind.SMALL)
    body.text("Layout local-pdf/v4 · apresentação ${model.details.photoLayout} · imagens até 1600 px", TextKind.SMALL)
    model.photoDisplays.forEach { (id, info) -> body.text("$id: codificada ${info.encodedWidth}×${info.encodedHeight} px; exibida ${info.displayWidth}×${info.displayHeight} px; orientação EXIF ${info.orientation}", TextKind.SMALL) }
    body.text("Manifesto: ${model.manifestId}", TextKind.SMALL)
    body.text("Chave da instalação (identidade civil não verificada): ${model.signerKeyId}", TextKind.SMALL)
    body.text("O hash do arquivo PDF final consta no recibo separado. Originais podem ser verificados no pacote completo.")
    for ((label, hex) in model.appendixHashes) {
        body.marker(PageItem.HashRow(label, hex))
        body.text(label, TextKind.SMALL, 0.0)
        body.text(hex, TextKind.SMALL)
    }
    body.flush()

    fun summary(offset: Int): List<RenderPage> {
        val index = PdfLayoutBuilder(renderer, model.reportId)
        index.text("SUMÁRIO", TextKind.HEADING)
        body.sections.forEach { (label, page) -> index.text("$label · página ${page + offset}") }
        index.text("Ambientes e cobertura", TextKind.HEADING)
        for ((room, page) in roomStarts) {
            index.text("${room.label} · ${room.coveragePercent}% · ${room.photos.size} fotos · página ${page + offset}")
        }
        index.text("Numeração corresponde às páginas físicas, incluindo capa e sumário.", TextKind.SMALL)
        index.text("Cobertura indica categorias fotografadas; não certifica ausência de defeitos.", TextKind.SMALL)
        index.flush()
        return index.pages
    }
    var index = summary(cover.size + 1)
    repeat(3) {
        val resolved = summary(cover.size + index.size)
        if (resolved.size == index.size) {
            return (cover + resolved + body.pages).mapIndexed { i, page -> page.copy(number = i + 1) }
        }
        index = resolved
    }
    throw LocalValidationException("page index did not converge")
}
