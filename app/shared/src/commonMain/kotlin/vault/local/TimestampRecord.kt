package vault.local

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalReport

// ACT timestamp token as an optional external step (RFC 3161).
// The token proves arrival at the contracted authority, never the shutter
// time. Offline the status stays ABSENT; with a registered token it becomes
// EXTERNAL and still requires the issuing ACT for any validity claim.
// No token is fabricated locally and no TSA is contacted by the app.
const val TIMESTAMP_SUFFIX = "rfc3161-token"

data class TimestampRecord(
    val reportId: String,
    val pdfSha256: String,
    val tokenSha256: String,
    val tokenSize: Long,
    val authority: String,
    val verification: String = EXTERNAL_SIGNATURE_UNVERIFIED,
)

fun timestampTokenPath(filesDir: String, reportId: String): String {
    require(safeArtifactId(reportId))
    return "$filesDir/vault/reports/$reportId-external-timestamp.$TIMESTAMP_SUFFIX"
}

fun resolveTrustedTime(tokenPresent: Boolean): String =
    if (tokenPresent) CERT_TIME_EXTERNAL else CERT_TIME_ABSENT

fun registerTimestampToken(
    filesDir: String,
    report: LocalReport,
    tokenBytes: ByteArray,
    authority: String,
    acknowledged: Boolean,
    fs: FileSystem = FileSystem.SYSTEM,
): TimestampRecord {
    require(acknowledged) { "explicit timestamp intent required" }
    require(report.status == REPORT_READY && report.pdfSha256 != null) { "report must be READY" }
    require(authority.isNotBlank() && authority.length <= 200) { "authority required" }
    require(tokenBytes.size in 32..1_048_576) { "invalid token size" }
    val pdfSha = report.pdfSha256
    require(pdfSha.matches(Regex("[0-9a-f]{64}")))
    val target = timestampTokenPath(filesDir, report.id).toPath()
    fs.createDirectories(target.parent!!)
    val tmp = (target.toString() + ".tmp").toPath()
    fs.write(tmp) { write(tokenBytes) }
    fs.atomicMove(tmp, target)
    return TimestampRecord(
        reportId = report.id,
        pdfSha256 = pdfSha,
        tokenSha256 = tokenBytes.toByteString().sha256().hex(),
        tokenSize = tokenBytes.size.toLong(),
        authority = authority.trim(),
    )
}

fun timestampPresent(filesDir: String, reportId: String, fs: FileSystem = FileSystem.SYSTEM): Boolean =
    fs.exists(timestampTokenPath(filesDir, reportId).toPath())
