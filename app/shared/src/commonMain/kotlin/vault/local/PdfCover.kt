package vault.local

import vault.local.PdfTone.*

internal fun reportCover(model: PdfModel, renderer: PdfTextMetrics): RenderPage {
    val cover = PdfLayoutBuilder(renderer, model.reportId)
    cover.marker(PageItem.Fill(PdfRect(0.0, 0.0, PDF_PAGE_WIDTH_PT, PDF_PAGE_HEIGHT_PT), PAPER))
    cover.put("REGISTRO DAS CONDIÇÕES APARENTES", TextKind.LABEL, 175.0)
    val title = if (model.details.profile == "technical") "Relatório técnico" else "Relatório"
    cover.put(title, TextKind.COVER_TITLE, 190.0)
    cover.put("de vistoria", TextKind.COVER_TITLE, 239.0)
    cover.put(model.inspectionLabel, TextKind.BODY, 315.0)

    val property = PdfColumn(pdfBody.x, 196.0)
    cover.put("IMÓVEL", TextKind.LABEL, 390.0, property, MUTED)
    val addressLines = cover.lines(model.address, TextKind.ADDRESS, property)
    if (addressLines.size * cover.height(TextKind.ADDRESS) <= 90) {
        cover.cell(model.address, TextKind.ADDRESS, 410.0, property, INK)
    } else {
        cover.cell("Endereço completo na identificação", TextKind.ADDRESS, 410.0, property, INK)
    }
    cover.fill(504.0, 21.0, PINE, property.copy(width = 168.0))
    cover.put("VISTORIA LACRADA NO DISPOSITIVO", TextKind.LABEL, 508.0,
        property.copy(x = property.x + 9, width = 152.0), WHITE)
    cover.markerAll(coverBuilding())

    cover.rule(592.0, PINE)
    val counts = listOf(
        model.rooms.size to "ambientes",
        model.rooms.sumOf { it.photos.size } to "registros visuais",
        model.rooms.sumOf { it.findings.size } to "observações",
    )
    counts.forEachIndexed { index, (count, label) ->
        val x = pdfBody.x + index * 151
        val column = PdfColumn(x, 135.0)
        if (index != 0) cover.marker(pdfLine(x - 20, 613.0, x - 20, 661.0, RULE))
        cover.put(count.pdfNumber(), TextKind.COUNTER, 607.0, column)
        cover.put(label, TextKind.CAPTION, 645.0, column, MUTED)
    }

    cover.put("IDENTIFICAÇÃO", TextKind.LABEL, 686.0, property, MUTED)
    cover.cell(model.reportId, TextKind.CAPTION, 706.0, PdfColumn(pdfBody.x, 225.0))
    val date = PdfColumn(320.0, pdfRight - 320)
    cover.put("DATA DA VISTORIA DECLARADA", TextKind.LABEL, 686.0, date, MUTED)
    val declaredDate = model.details.context.inspectionDate
    cover.put(if (declaredDate.isBlank()) "Não informada" else declaredDate.split('-').asReversed().joinToString("/"),
        TextKind.CAPTION, 706.0, date)
    cover.cell("Hora e localização são informações do dispositivo.\nO compartilhamento não registra concordância.",
        TextKind.CAPTION, 741.0)
    cover.flush()
    return cover.pages.single().copy(outlineTitle = "$title de vistoria", isCover = true)
}

private fun coverBuilding(): List<PageItem> = buildList {
    fun edges(vararg points: Pair<Int, Int>, tone: PdfTone = PINE) {
        add(PageItem.Path(points.map { (x, y) -> PdfPoint(x.toDouble(), y.toDouble()) }, tone, 0.65))
    }
    add(PageItem.Path(listOf(PdfPoint(306.0, 423.0), PdfPoint(376.0, 388.0),
        PdfPoint(519.0, 388.0), PdfPoint(448.0, 423.0)), SAGE))
    edges(288 to 554, 538 to 554)
    edges(306 to 554, 306 to 423, 376 to 388, 519 to 388, 519 to 520, 448 to 554, 448 to 423, 306 to 423)
    edges(448 to 423, 519 to 388)
    edges(306 to 489, 448 to 489, 519 to 454)
    edges(328 to 554, 328 to 506, 364 to 506, 364 to 554)
    edges(356 to 530, 356 to 535)
    edges(380 to 506, 425 to 506, 425 to 534, 380 to 534, 380 to 506)
    edges(402 to 506, 402 to 534, tone = RULE)
    edges(324 to 474, 324 to 440, 425 to 440, 425 to 474)
    for (x in listOf(349, 374, 400)) edges(x to 440, x to 474, tone = RULE)
    edges(315 to 489, 315 to 474, 433 to 474, 433 to 489)
    edges(315 to 481, 433 to 481)
    for (x in listOf(355, 394)) edges(x to 474, x to 489)
    edges(465 to 469, 465 to 428, 502 to 410, 502 to 450, 465 to 469)
    edges(465 to 532, 465 to 495, 502 to 477, 502 to 514, 465 to 532)
    edges(483 to 420, 483 to 460, tone = RULE)
    edges(483 to 486, 483 to 523, tone = RULE)
}
