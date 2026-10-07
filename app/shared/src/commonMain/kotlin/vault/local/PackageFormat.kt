package vault.local

// QR reference: identity triple for lookup and local import. Scanning
// it never certifies bytes or photos; only package verification does.
data class QrRef(val reportId: String, val manifestSha256: String)

fun parseQrIdentity(raw: String): QrRef? {
    val trimmed = raw.trim()
    val prefix = "vault-report/v1/"
    if (!trimmed.startsWith(prefix)) return null
    val rest = trimmed.removePrefix(prefix).split("/")
    if (rest.size != 2 || rest[0].isBlank()) return null
    val manifest = rest[1]
    if (manifest.length != 64 || manifest.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
    return QrRef(rest[0], manifest)
}

// Offline evidence package (directory). The published format both the app
// and the independent verifier implement:
//
//   <package>/report.pdf          final PDF bytes
//   <package>/receipt.json        {"receipt": {...}, "signature": hex}
//   <package>/manifest.json       canonical manifest bytes, verbatim
//   <package>/manifest.sig.json   {"keyId","algorithm","signature"}
//   <package>/signer.json         {"keyId","publicKey","hardwareBacked"}
//   <package>/originals/<id>.bin  immutable originals by evidence id
//
// Identity (QR) never proves integrity: only the receipt's PDF hash and
// the manifest signature do, recomputed from the shipped bytes.
const val PACKAGE_VERSION = 1

enum class FileStatus { OK, MISSING, MODIFIED }

enum class IssuerTrust { UNKNOWN, PINNED, CERTIFIED }

data class PackageVerdict(
    val pdf: FileStatus,
    val receipt: FileStatus,
    val manifest: FileStatus,
    val evidencesChecked: Int,
    val evidencesMissing: List<String>,
    val evidencesModified: List<String>,
    val signatureValid: Boolean,
    val issuerTrust: IssuerTrust,
    val signerKeyId: String? = null,
) {
    fun accepted(): Boolean =
        pdf == FileStatus.OK && receipt == FileStatus.OK && manifest == FileStatus.OK &&
            evidencesMissing.isEmpty() && evidencesModified.isEmpty() && signatureValid
}
