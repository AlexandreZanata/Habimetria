package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.ByteString.Companion.toByteString
import vault.local.db.LocalReport

// Honest external signing: the app never embeds PAdES itself.
// Android PdfDocument has no incremental-update/ByteRange signer, so a
// locally drawn PDF cannot become PAdES by renaming. These handoffs
// prepare hashes/instructions for an explicit user-driven external step
// (ITI-compatible signer, Gov.br Assinador, contracted ACT/TSA) while the
// local READY PDF stays offline, preserved and verifiable.
const val EXTERNAL_SIGNATURE_UNVERIFIED = "EXTERNAL_UNVERIFIED"
const val EXTERNAL_METHOD_PADES = "external-pades/v1"
const val EXTERNAL_METHOD_GOVBR = "govbr-manual/v1"
const val EXTERNAL_METHOD_TIMESTAMP = "act-manual/v1"

data class ExternalHandoff(
    val reportId: String,
    val pdfSha256: String,
    val manifestSha256: String,
    val pdfSize: Long,
    val method: String,
    val instructions: String,
)

data class ExternalRecord(
    val reportId: String,
    val method: String,
    val originalPdfSha256: String,
    val signedPdfSha256: String,
    val signedSize: Long,
    val verification: String = EXTERNAL_SIGNATURE_UNVERIFIED,
)

fun externalInstructions(method: String): String = when (method) {
    EXTERNAL_METHOD_PADES ->
        "Export the READY PDF and sign a copy with your authorized A1 holder " +
            "certificate in an ITI-compatible external signer (PAdES RB/RT, DOC-ICP-15; " +
            "ask the signer to enable DocMDP, otherwise VALIDAR may report Indeterminate). " +
            "Keep the original READY file. Confirm the signed copy at ITI VALIDAR " +
            "(https://validar.iti.gov.br/). " +
            "This app does not embed PAdES and never marks an external file as locally verified."
    EXTERNAL_METHOD_GOVBR ->
        "Optional Gov.br signing is a manual external service at the official " +
            "Assinador (https://assinador.iti.br/). It requires your Gov.br prata or ouro " +
            "account; bronze cannot sign (Portaria SGD/MGI 11.230/2025, Decreto 10.543/2020). " +
            "Upload a copy of the READY PDF there; keep the local original. Opening or " +
            "downloading never counts as a signature. No Gov.br credential is entered " +
            "in this app and no Gov.br API is called; app integration would require " +
            "separate credentialing which this delivery does not claim."
    EXTERNAL_METHOD_TIMESTAMP ->
        "Optional ACT timestamp requires your contracted time-stamp authority " +
            "(ICP-Brasil ACT, RFC 3161 TSP, DOC-ICP-13). Request a token over the PDF " +
            "SHA-256 there; the token proves arrival at the authority, not the shutter " +
            "time (ITI timestamp guidance). Offline device time is never a certified " +
            "timestamp. Timestamps are facultative for ICP-Brasil signatures."
    else -> error("unknown external method")
}

fun prepareExternalHandoff(report: LocalReport, pdfSize: Long, method: String): ExternalHandoff {
    require(report.status == REPORT_READY && report.pdfSha256 != null) { "report must be READY" }
    require(method == EXTERNAL_METHOD_PADES || method == EXTERNAL_METHOD_GOVBR || method == EXTERNAL_METHOD_TIMESTAMP) {
        "unknown external method"
    }
    require(pdfSize in 1..MAX_ORIGINAL_BYTES) { "invalid PDF size" }
    require(report.pdfSha256.matches(Regex("[0-9a-f]{64}")))
    require(report.manifestSha256.matches(Regex("[0-9a-f]{64}")))
    return ExternalHandoff(report.id, report.pdfSha256, report.manifestSha256, pdfSize, method, externalInstructions(method))
}

fun externalSignedPath(filesDir: String, reportId: String, method: String): String {
    require(safeArtifactId(reportId))
    val suffix = when (method) {
        EXTERNAL_METHOD_PADES -> "pades"
        EXTERNAL_METHOD_GOVBR -> "govbr"
        EXTERNAL_METHOD_TIMESTAMP -> "timestamped"
        else -> error("unknown external method")
    }
    return "$filesDir/vault/reports/$reportId-external-$suffix.pdf"
}

// Registers an externally signed copy without touching READY bytes.
// The original PDF and receipt stay authoritative locally; the signed
// copy is a distinct file whose PAdES/Gov.br/ACT validity must be checked
// in its external verifier (e.g. ITI VALIDAR), never here.
fun registerExternalSignedCopy(
    filesDir: String,
    report: LocalReport,
    method: String,
    signedBytes: ByteArray,
    acknowledged: Boolean = true,
    fs: FileSystem = FileSystem.SYSTEM,
): ExternalRecord {
    require(acknowledged) { "explicit external signing intent required" }
    require(report.status == REPORT_READY && report.pdfSha256 != null) { "report must be READY" }
    require(method == EXTERNAL_METHOD_PADES || method == EXTERNAL_METHOD_GOVBR || method == EXTERNAL_METHOD_TIMESTAMP)
    requirePadesReadyPdf(signedBytes)
    val originalSha = report.pdfSha256
    val signedSha = signedBytes.toByteString().sha256().hex()
    require(signedSha != originalSha) { "signed copy must differ from READY bytes" }
    val target = externalSignedPath(filesDir, report.id, method).toPath()
    fs.createDirectories(target.parent!!)
    val tmp = (target.toString() + ".tmp").toPath()
    fs.write(tmp) { write(signedBytes) }
    fs.atomicMove(tmp, target)
    return ExternalRecord(report.id, method, originalSha, signedSha, signedBytes.size.toLong())
}

// Independent local check: READY preserved, signed copy distinct and PDF-shaped.
// Never claims external validity; EXTERNAL_UNVERIFIED always remains.
fun verifyExternalCopy(
    filesDir: String,
    report: LocalReport,
    method: String,
    fs: FileSystem = FileSystem.SYSTEM,
): ExternalRecord? {
    if (report.status != REPORT_READY || report.pdfSha256 == null) return null
    val path = externalSignedPath(filesDir, report.id, method).toPath()
    if (!fs.exists(path)) return null
    val bytes = fs.read(path) { readByteArray() }
    if (!isPadesReadyPdf(bytes)) return null
    val signedSha = bytes.toByteString().sha256().hex()
    if (signedSha == report.pdfSha256) return null
    val readyBytes = runCatching {
        fs.read(ReportArtifacts(filesDir, fs).pdf(report.id).toPath()) { readByteArray() }
    }.getOrNull() ?: return null
    if (readyBytes.toByteString().sha256().hex() != report.pdfSha256) return null
    return ExternalRecord(report.id, method, report.pdfSha256, signedSha, bytes.size.toLong())
}
