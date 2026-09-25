package vault.pdf

// Platform PDF pipeline. Android implements in Kotlin (PdfDocument,
// zxing); iOS in Swift (PDFKit, CoreImage) injected from the host.
// Shared code owns the layout model + flow, never pixels.
interface PlatformPdf {
    fun render(doc: PdfDoc, seal: SealBlock): ByteArray
    fun qr(content: String, sizePx: Int = 256): ByteArray
    fun share(path: String, mime: String)
}
