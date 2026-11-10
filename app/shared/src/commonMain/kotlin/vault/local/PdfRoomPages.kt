package vault.local

internal fun PdfLayoutBuilder.drawRoom(
    room: PdfRoomSection, index: Int, total: Int, maxPhotoHeight: Double, dimsOf: (String) -> ImageDims?,
) {
    val number = (index + 1).pdfNumber()
    val label = "03.$number / REGISTRO POR AMBIENTE"
    val title = PdfColumn(pdfBody.x, pdfBody.width - 134)
    val coverage = buildList {
        add("Cobertura por categorias: ${room.coveragePercent}%")
        if (room.missingCategories.isNotEmpty()) add("Categorias sem foto: ${room.missingCategories.joinToString(", ")}")
        if (room.notApplicableCategories.isNotEmpty()) {
            add("Declaradas não aplicáveis: ${room.notApplicableCategories.joinToString(", ")}")
        }
    }.joinToString("\n")
    val findingsByPhoto = room.findings.groupBy { it.evidenceId }
    fun start(count: Int) {
        heading(label, room.label, column = title)
        val message = when (count) {
            0 -> "SEM APONTAMENTO"
            1 -> "01 OBSERVAÇÃO"
            else -> "${count.pdfNumber()} OBSERVAÇÕES"
        }
        val badgeWidth = if (count == 1) 87.0 else 118.0
        val badge = PdfColumn(pdfRight - badgeWidth, badgeWidth, PdfAlign.CENTER)
        fill(128.0, 21.0, if (count == 0) PdfTone.SAGE else PdfTone.PINE, badge)
        put(message, TextKind.LABEL, 132.0, badge, if (count == 0) PdfTone.PINE else PdfTone.WHITE)
        spaceTo(170.0)
    }

    if (room.photos.isEmpty()) {
        start(room.findings.size)
        text("Nenhum registro visual neste ambiente.")
        text(coverage, TextKind.SMALL)
        flush()
        return
    }

    for ((photoIndex, photo) in room.photos.withIndex()) {
        val linked = findingsByPhoto[photo.evidenceId].orEmpty() +
            if (photoIndex == 0) findingsByPhoto[null].orEmpty() else emptyList()
        start(linked.size)
        val ref = evidenceReference(photo.sequence)
        val position = "$number DE ${total.pdfNumber()} AMBIENTES"
        put(position, TextKind.FOOTER, y, PdfColumn(pdfRight - 115, 115.0, PdfAlign.RIGHT))
        text("$ref / ${reportShotLabel(photo.category).uppercase()}", TextKind.LABEL, 13.0,
            PdfColumn(pdfBody.x, pdfBody.width - 129))
        val dims = dimsOf(photo.evidenceId) ?: throw LocalValidationException("undecodable photo: ${photo.evidenceId}")
        val recordPage = currentNumber
        photo(photo, dims, maxPhotoHeight)
        if (currentNumber == recordPage && maxPhotoHeight == PDF_MAX_PHOTO_HEIGHT_PT) spaceTo(655.0)
        if (linked.isEmpty()) {
            panel("SEM OBSERVAÇÃO VINCULADA",
                "Nenhuma observação cadastrada para esta imagem. A ausência de apontamentos não equivale à aprovação do ambiente.")
        }
        for (finding in linked) {
            marker(PageItem.Finding(finding))
            panel("${finding.observationReference()} / ${finding.preset.uppercase()} / $ref",
                finding.note?.takeIf { it.isNotBlank() } ?: "Observação registrada sem nota adicional.")
        }
        if (photoIndex == 0) text(coverage, TextKind.SMALL)
        flush()
    }
}

internal fun PdfFindingEntry.observationReference(): String =
    if (number > 0) "OBS-${number.pdfNumber()}" else "OBSERVAÇÃO"
