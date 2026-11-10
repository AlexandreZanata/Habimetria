package vault.local

import vault.local.PdfTone.*

internal fun reportSummary(
    model: PdfModel, renderer: PdfTextMetrics, roomStarts: List<Int>, pageOffset: Int,
): List<RenderPage> {
    val layout = PdfLayoutBuilder(renderer, model.reportId)
    val photoCount = model.rooms.sumOf { it.photos.size }
    val findingCount = model.rooms.sumOf { it.findings.size }
    layout.heading("02 / VISÃO DO CONJUNTO", "Síntese e navegação",
        "${model.rooms.size} ambientes · $photoCount registros visuais · $findingCount observações vinculadas")
    layout.spaceTo(211.0)
    layout.drawRoomIndex(model.rooms, roomStarts, pageOffset)
    layout.gap(25.0)
    layout.drawFindingIndex(model.rooms)
    layout.gap(27.0)
    layout.drawCoverage(model.rooms)
    layout.flush()
    return layout.pages
}

private fun PdfLayoutBuilder.drawRoomIndex(rooms: List<PdfRoomSection>, starts: List<Int>, offset: Int) {
    val ordinal = PdfColumn(97.0, 27.0)
    val name = PdfColumn(127.0, 184.0)
    val evidence = PdfColumn(323.0, 78.0)
    val findings = PdfColumn(403.0, 48.0, PdfAlign.CENTER)
    val page = PdfColumn(470.0, 52.0, PdfAlign.RIGHT)
    fun header() {
        fill(y, 28.0, PINE)
        put("AMBIENTE", TextKind.LABEL, y + 7, PdfColumn(ordinal.x, 200.0), WHITE)
        put("REGISTRO", TextKind.LABEL, y + 7, evidence, WHITE)
        put("OBS.", TextKind.LABEL, y + 7, findings, WHITE)
        put("PÁG.", TextKind.LABEL, y + 7, page, WHITE)
        gap(28.0)
    }
    header()
    rooms.forEachIndexed { index, room ->
        val nameLines = lines(room.label, TextKind.BODY, name)
        val rowHeight = maxOf(29.0, nameLines.size * height(TextKind.BODY) + 10)
        val pageBefore = currentNumber
        ensure(rowHeight)
        if (currentNumber != pageBefore) header()
        if (index % 2 == 0) fill(y, rowHeight, PAPER)
        put((index + 1).pdfNumber(), TextKind.SMALL, y + 7, ordinal)
        nameLines.forEachIndexed { i, line -> put(line, TextKind.BODY, y + 5 + i * height(TextKind.BODY), name) }
        val ref = room.photos.firstOrNull()?.let { evidenceReference(it.sequence) } ?: "—"
        val count = room.photos.size
        put(if (count > 1) "$ref +${count - 1}" else ref, TextKind.CAPTION, y + 6, evidence, PINE)
        put(room.findings.size.pdfNumber(), TextKind.CAPTION, y + 6, findings)
        put((starts[index] + offset).pdfNumber(), TextKind.BODY, y + 5, page, PINE)
        gap(rowHeight)
    }
    rule()
}

private fun PdfLayoutBuilder.drawFindingIndex(rooms: List<PdfRoomSection>) {
    ensure(65.0)
    text("Observações registradas", TextKind.HEADING, 10.0)
    if (rooms.none { it.findings.isNotEmpty() }) {
        text("Nenhuma observação cadastrada.", TextKind.CAPTION)
    }
    val label = PdfColumn(158.0, 288.0)
    val evidence = PdfColumn(453.0, pdfRight - 453, PdfAlign.RIGHT)
    for (finding in rooms.flatMap { it.findings }) {
        val rows = lines(finding.preset, TextKind.CAPTION, label)
        val rowHeight = maxOf(33.0, rows.size * height(TextKind.CAPTION) + 12)
        ensure(rowHeight)
        put(finding.observationReference(), TextKind.LABEL, y + 3)
        cell(finding.evidenceRef, TextKind.LABEL, y + 3, evidence)
        rows.forEachIndexed { i, line -> put(line, TextKind.CAPTION, y + 3 + i * height(TextKind.CAPTION), label) }
        gap(rowHeight)
        rule()
    }
}

private fun PdfLayoutBuilder.drawCoverage(rooms: List<PdfRoomSection>) {
    val categories = ShotCategory.all()
    val coverage = categories.map { category ->
        val label = reportShotLabel(category.name)
        val eligible = rooms.filter { label !in it.notApplicableCategories }
        val photographed = eligible.count { room -> room.photos.any { it.category == category.name } }
        Triple(label, photographed, eligible.size)
    }
    ensure(135.0)
    val total = "${coverage.sumOf { it.second }} de ${coverage.sumOf { it.third }} categorias aplicáveis"
    put(total, TextKind.SMALL, y + 4, pdfBody.copy(align = PdfAlign.RIGHT))
    text("Cobertura da vistoria", TextKind.HEADING, 13.0)
    val gutter = 6.0
    val width = (pdfBody.width - (coverage.size - 1) * gutter) / coverage.size
    coverage.forEachIndexed { i, (label, captured, eligible) ->
        val card = PdfColumn(pdfBody.x + i * (width + gutter), width, PdfAlign.CENTER)
        fill(y, 63.0, if (captured > 0) SAGE else PAPER, card)
        put(label, TextKind.FOOTER, y + 9, card, if (captured > 0) PINE else MUTED)
        put("$captured / $eligible", TextKind.VALUE, y + 27, card, if (captured > 0) PINE else INK)
    }
    gap(80.0)
    text("A cobertura considera categorias distintas por ambiente. Categorias declaradas não aplicáveis ficam fora da contagem. A cobertura fotográfica não certifica ausência de defeitos.",
        TextKind.CAPTION)
}
