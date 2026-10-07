package vault.local

import kotlinx.serialization.json.*
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalReport

internal class ReportArtifacts(private val filesDir: String, private val fs: FileSystem) {
    fun directory() = "$filesDir/vault/reports"
    fun pdf(id: String): String { require(safeArtifactId(id)); return "${directory()}/$id.pdf" }
    fun receipt(id: String): String { require(safeArtifactId(id)); return "${directory()}/$id-receipt.json" }

    fun publish(path: String, bytes: ByteArray) {
        val target = path.toPath()
        fs.createDirectories(target.parent!!)
        val tmp = "$path.tmp".toPath()
        fs.write(tmp) { write(bytes) }
        fs.atomicMove(tmp, target)
    }

    fun hash(path: String): String = fs.read(path.toPath()) { readByteArray() }.toByteString().sha256().hex()

    fun receiptValid(report: LocalReport, crypto: InstallationSigner, publicKey: ByteArray): Boolean = try {
        val path = receipt(report.id).toPath()
        require((fs.metadataOrNull(path)?.size ?: 0) in 1..16_384) { "invalid receipt size" }
        val root = Json.parseToJsonElement(fs.read(path) { readUtf8() }).jsonObject
        val body = root.getValue("receipt").jsonObject
        fun value(name: String) = body.getValue(name).jsonPrimitive.content
        require(value("algorithm") == SEAL_ALGORITHM && value("purpose") == RECEIPT_PURPOSE)
        require(value("formatVersion").toInt() == RECEIPT_FORMAT_VERSION)
        require(value("reportId") == report.id && value("manifestId") == report.manifestId)
        require(value("manifestSha256") == report.manifestSha256 && value("pdfSha256") == report.pdfSha256)
        require(value("signerKeyId") == report.signerKeyId && keyFingerprint(publicKey) == report.signerKeyId)
        val signature = root.getValue("signature").jsonPrimitive.content
        require(signature == report.receiptSignature) { "receipt signature differs from stored record" }
        crypto.verify(publicKey, receiptBytes(report.id, report.manifestId,
            report.manifestSha256, report.pdfSha256!!, report.signerKeyId), signature.decodeHex().toByteArray())
    } catch (_: Exception) {
        false
    }
}
