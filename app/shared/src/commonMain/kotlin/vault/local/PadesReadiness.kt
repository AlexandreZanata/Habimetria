package vault.local

// PAdES readiness for a READY PDF without embedding a signature locally.
// Android PdfDocument and the current iOS writer have no incremental-update
// or ByteRange signer, so this device never manufactures PAdES bytes.
// Readiness means the READY file is a complete PDF that an authorized
// external signer (holder A1, Gov.br Assinador, contracted ACT/TSA) can
// ingest, while the local original stays preserved and verifiable offline.
// Any validity claim belongs to ITI VALIDAR or the issuing authority.
fun requirePadesReadyPdf(bytes: ByteArray): ByteArray {
    require(bytes.size in 1..MAX_ORIGINAL_BYTES.toInt()) { "invalid PDF size" }
    require(bytes.size >= 8) { "not a PDF file" }
    val header = bytes.sliceArray(0 until 5).decodeToString()
    require(header == "%PDF-") { "not a PDF file" }
    val tail = bytes.takeLast(4096).toByteArray().decodeToString()
    require(tail.contains("%%EOF")) { "incomplete PDF" }
    return bytes
}

fun isPadesReadyPdf(bytes: ByteArray): Boolean =
    runCatching { requirePadesReadyPdf(bytes) }.isSuccess
