package vault.local

// Paginates the whole model. Every photo and finding lands exactly once;
// pages carry sequential numbers starting at 1 (cover).
fun paginateModel(
    model: PdfModel,
    dimsOf: (evidenceId: String) -> ImageDims?,
): List<RenderPage> {
    val pages = mutableListOf<RenderPage>()
    var number = 0
    fun newPage(): MutableList<PageItem> {
        number++
        return mutableListOf()
    }
    var current = newPage()
    fun flush() {
        pages.add(RenderPage(number, model.reportId, current.toList()))
        current = newPage()
    }
    // Cover.
    current.add(PageItem.Text(TextKind.TITLE, "Relatório de Vistoria"))
    current.add(PageItem.Text(TextKind.BODY, model.address))
    current.add(PageItem.Text(TextKind.BODY, "Inspeção ${model.inspectionId}"))
    current.add(PageItem.Text(TextKind.BODY, "Lacrado (dispositivo): ${formatSealedAtDevice(model.sealedAtDevice)}"))
    current.add(PageItem.Text(TextKind.BODY, "Manifesto ${model.manifestSha256.take(16)}…"))
    current.add(PageItem.Text(TextKind.BODY, "Chave ${model.signerKeyId.take(16)}…"))
    current.add(PageItem.Qr(qrContent(model.reportId, model.manifestSha256)))
    current.add(
        PageItem.Text(
            TextKind.SMALL,
            "O QR identifica o relatório. A integridade se verifica no app com o recibo assinado.",
        ),
    )
    flush()
    // Rooms.
    for (room in model.rooms) {
        current.add(PageItem.Text(TextKind.HEADING, "${room.label} — ${room.coveragePercent}%"))
        var cursorY = PDF_MARGIN_TOP_PT + 24.0
        for (photo in room.photos) {
            val dims = dimsOf(photo.evidenceId) ?: ImageDims(4, 3)
            val aspect = dims.width.toDouble() / dims.height.toDouble().coerceAtLeast(1.0)
            val height = (contentWidthPt() / aspect).coerceAtMost(PDF_MAX_PHOTO_HEIGHT_PT)
            val width = height * aspect
            if (cursorY + height + 30.0 > bottomLimitPt()) {
                flush()
                cursorY = PDF_MARGIN_TOP_PT
            }
            current.add(
                PageItem.Photo(
                    photo,
                    PdfRect(PDF_MARGIN_LEFT_PT + (contentWidthPt() - width) / 2, cursorY, width, height),
                ),
            )
            // Image plus single-line caption and claim (drawn by platform).
            cursorY += height + 30.0
        }
        for (finding in room.findings) current.add(PageItem.Finding(finding))
        if (current.isNotEmpty()) flush()
    }
    // Methodology.
    current.add(PageItem.Text(TextKind.HEADING, "Metodologia"))
    model.methodology.forEach { current.add(PageItem.Text(TextKind.BODY, it)) }
    flush()
    // Appendix: every digest in full.
    current.add(PageItem.Text(TextKind.HEADING, "Apêndice — resumos"))
    current.add(PageItem.Text(TextKind.BODY, "Manifesto ${model.manifestId}"))
    for ((label, hex) in model.appendixHashes) {
        if (current.size >= 40) flush()
        current.add(PageItem.HashRow(label, hex))
    }
    flush()
    return pages.dropLastWhile { it.items.isEmpty() }
}
