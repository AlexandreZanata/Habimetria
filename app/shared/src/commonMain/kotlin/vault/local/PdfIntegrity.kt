package vault.local

internal fun PdfModel.drawIntegrity(layout: PdfLayoutBuilder) = with(layout) {
    heading("04 / IDENTIFICAÇÃO DOS ARQUIVOS", "Integridade e autoria",
        "Referências dos arquivos que compõem o registro.")
    spaceTo(208.0)
    val identity = PdfColumn(213.0, pdfRight - 227)
    val qr = qrContent(reportId, manifestSha256)
    fill(y, 119.0, PdfTone.PAPER)
    marker(PageItem.Qr(qr, PdfRect(pdfBody.x + 12, y + 11, 96.0, 96.0)))
    put("IDENTIDADE DO RELATÓRIO", TextKind.LABEL, y + 11, identity)
    cell(reportId, TextKind.CAPTION, y + 29, identity)
    put("SHA-256 DO MANIFESTO", TextKind.LABEL, y + 51, identity)
    cell(manifestSha256.chunked(32).joinToString("\n"), TextKind.CAPTION, y + 65, identity)
    put("QR de identificação. A integridade do PDF é verificada pelo recibo.", TextKind.FOOTER, y + 96, identity)
    gap(135.0)

    text("O manifesto e os arquivos originais acompanham o pacote completo. O hash do PDF final consta no recibo separado.",
        TextKind.CAPTION, 14.0)
    val photos = rooms.flatMap { it.photos }.associateBy { it.evidenceId }
    for ((label, hex) in appendixHashes) {
        marker(PageItem.HashRow(label, hex))
        if (label == "manifest") continue // Digest already printed in the identity panel.
        val id = label.removePrefix("evidence ")
        val photo = photos[id]
        ensure(37.0)
        text(if (photo == null) label else "${evidenceReference(photo.sequence)} / $id", TextKind.LABEL, 2.0)
        text(hex, TextKind.SMALL, 2.0)
        gap(5.0)
        rule()
        gap(4.0)
    }
    gap(10.0)
    panel("ESTADO DO REGISTRO", listOf(
        "Lacre informado pelo dispositivo: $sealedAtDevice",
        "Manifesto: $manifestId",
        "Chave da instalação: $signerKeyId",
        "Identidade civil não verificada. Declarações posteriores acompanham o ZIP.",
        "Layout local-pdf/v5 · apresentação ${details.photoLayout} · imagens até 1600 px",
    ).joinToString("\n"), PdfTone.PAPER, accent = false)
    text("Hashes identificam bytes. A assinatura local identifica a instalação. A cena, a identidade civil e a concordância das partes não são certificadas.",
        TextKind.CAPTION)
    flush()
}
