package vault.local

const val RECEIPT_FORMAT_VERSION = 2
const val RECEIPT_PURPOSE = "local-receipt/v1"
const val REPORT_RENDERING = "RENDERING"
const val REPORT_READY = "READY"
const val REPORT_FAILED = "FAILED"

class IncompleteReportException(val missing: List<String>) :
    IllegalStateException("missing originals for ${missing.size} sealed evidences")

// Detached receipt: binds Report, Manifest and the exact PDF bytes.
// The PDF never embeds its own whole-file hash; integrity lives here.
data class LocalReceipt(
    val reportId: String,
    val manifestId: String,
    val manifestSha256: String,
    val pdfSha256: String,
    val signerKeyId: String,
    val signature: String,
)

fun receiptBytes(
    reportId: String,
    manifestId: String,
    manifestSha256: String,
    pdfSha256: String,
    signerKeyId: String,
    formatVersion: Int = RECEIPT_FORMAT_VERSION,
): ByteArray = buildString {
    require(formatVersion in 1..RECEIPT_FORMAT_VERSION)
    append("{\"algorithm\":")
    append(quote(SEAL_ALGORITHM))
    if (formatVersion >= 2) append(",\"declarationsRequired\":true")
    append(",\"formatVersion\":")
    append(formatVersion)
    append(",\"manifestId\":")
    append(quote(manifestId))
    append(",\"manifestSha256\":")
    append(quote(manifestSha256))
    append(",\"pdfSha256\":")
    append(quote(pdfSha256))
    append(",\"purpose\":")
    append(quote(RECEIPT_PURPOSE))
    append(",\"reportId\":")
    append(quote(reportId))
    append(",\"signerKeyId\":")
    append(quote(signerKeyId))
    append("}")
}.encodeToByteArray()

internal fun quoteJsonString(raw: String): String = buildString {
    append('"')
    for (ch in raw) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u" + ch.code.toString(16).padStart(4, '0'))
            } else {
                append(ch)
            }
        }
    }
    append('"')
}

// Shared with ReportGenerator (split for the file-size limit).
internal fun quote(raw: String): String = quoteJsonString(raw)

data class ReportVerification(
    val pdfIntact: Boolean,
    val manifestOk: Boolean,
    val receiptOk: Boolean,
    val originalsIntact: Boolean = true,
    val declarationsIntact: Boolean = true,
) {
    fun allOk(): Boolean = pdfIntact && manifestOk && receiptOk && originalsIntact && declarationsIntact
}
