package vault.local

private object DefaultPdfMetrics : PdfTextMetrics

fun paginateModel(model: PdfModel, dimsOf: (String) -> ImageDims?): List<RenderPage> =
    paginateModel(model, DefaultPdfMetrics, dimsOf)

fun paginateModel(model: PdfModel, renderer: PdfTextMetrics, dimsOf: (String) -> ImageDims?): List<RenderPage> {
    val cover = PdfLayoutBuilder(renderer, model.reportId)
    cover.text("RELATÓRIO DE VISTORIA", TextKind.TITLE, 14.0)
    cover.text("Registro fotográfico do imóvel", TextKind.HEADING)
    cover.text(model.address)
    cover.text("Vistoria: ${model.inspectionId}", TextKind.SMALL)
    cover.text("Lacre informado pelo dispositivo: ${model.sealedAtDevice}")
    cover.text("${model.rooms.size} ambientes · ${model.rooms.sumOf { it.photos.size }} fotografias · " +
        "${model.rooms.sumOf { it.findings.size }} observações")
    cover.gap(20.0)
    cover.qr(qrContent(model.reportId, model.manifestSha256))
    cover.text("O QR identifica o relatório. Para verificar seus bytes, utilize o PDF e o recibo assinado.", TextKind.SMALL)
    cover.text("Hora e localização são informações do dispositivo. Compartilhamento não significa concordância.", TextKind.SMALL)
    cover.flush()

    val body = PdfLayoutBuilder(renderer, model.reportId)
    val roomStarts = mutableListOf<Pair<PdfRoomSection, Int>>()
    for (room in model.rooms) {
        body.flush()
        roomStarts.add(room to body.currentNumber)
        body.text(room.label, TextKind.HEADING)
        body.text("Cobertura por categorias: ${room.coveragePercent}% · ${room.photos.size} fotos", TextKind.SMALL)
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
    body.text("Metodologia e limitações", TextKind.HEADING)
    model.methodology.forEach { body.text(it) }
    body.flush()
    body.text("Verificação de integridade", TextKind.HEADING)
    body.text("Relatório: ${model.reportId}", TextKind.SMALL)
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
        index.text("Ambientes e cobertura", TextKind.HEADING)
        for ((room, page) in roomStarts) {
            index.text("${room.label} · ${room.coveragePercent}% · ${room.photos.size} fotos · página ${page + offset}")
        }
        index.text("Cobertura indica categorias fotografadas; não certifica ausência de defeitos.", TextKind.SMALL)
        index.flush()
        return index.pages
    }
    var index = summary(cover.pages.size + 1)
    repeat(3) {
        val resolved = summary(cover.pages.size + index.size)
        if (resolved.size == index.size) {
            return (cover.pages + resolved + body.pages).mapIndexed { i, page -> page.copy(number = i + 1) }
        }
        index = resolved
    }
    throw LocalValidationException("page index did not converge")
}
