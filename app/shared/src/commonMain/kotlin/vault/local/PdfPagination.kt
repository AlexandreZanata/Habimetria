package vault.local

private object DefaultPdfMetrics : PdfTextMetrics

fun paginateModel(model: PdfModel, dimsOf: (String) -> ImageDims?): List<RenderPage> =
    paginateModel(model, DefaultPdfMetrics, dimsOf)

fun paginateModel(model: PdfModel, renderer: PdfTextMetrics, dimsOf: (String) -> ImageDims?): List<RenderPage> {
    val cover = reportCover(model, renderer)
    val identity = PdfLayoutBuilder(renderer, model.reportId)
    model.drawReportDetails(identity)

    val body = PdfLayoutBuilder(renderer, model.reportId)
    val maxPhotoHeight = if (model.details.photoLayout == "detail") 520.0 else PDF_MAX_PHOTO_HEIGHT_PT
    val roomStarts = model.rooms.mapIndexed { index, room ->
        val start = body.currentNumber
        body.drawRoom(room, index, model.rooms.size, maxPhotoHeight, dimsOf)
        start
    }
    model.details.drawReportConclusions(body)
    model.drawIntegrity(body)

    // Index page cells have fixed dimensions regardless of the printed page number.
    val prefix = listOf(cover) + identity.pages
    val summarySize = reportSummary(model, renderer, roomStarts, 0).size
    val summary = reportSummary(model, renderer, roomStarts, prefix.size + summarySize)
    val pages = prefix + summary + body.pages
    return pages.mapIndexed { index, page ->
        page.copy(number = index + 1).withChrome(model, renderer, pages.size)
    }
}
