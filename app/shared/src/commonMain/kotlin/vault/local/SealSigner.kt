package vault.local

import okio.ByteString.Companion.toByteString

// Installation signing, one key per install, ECDSA P-256 over SHA-256.
//
// Wire format (cross-platform contract, also used by the L7 verifier):
// - public key: 65 bytes, uncompressed 0x04 || X(32) || Y(32), big-endian.
// - signature: 64 bytes, raw R(32) || S(32), big-endian (never DER).
// - keyId: hex SHA-256 of the uncompressed public key.
//
// No global secret is embedded: Android uses Keystore (StrongBox/TEE),
// iOS uses Keychain/Secure Enclave, JVM uses a validation-only file key.
// A verifier trusts a keyId only through independent comparison/pinning,
// never because it arrived inside a package.
const val SEAL_ALGORITHM = "ECDSA-P256-SHA256-raw64"

interface InstallationSigner {
    val keyId: String
    val hardwareBacked: Boolean
    val validationOnly: Boolean
    fun publicKeyUncompressed(): ByteArray
    fun sign(bytes: ByteArray): ByteArray
    fun verify(publicKeyUncompressed: ByteArray, bytes: ByteArray, signature: ByteArray): Boolean
}

expect fun platformSigner(filesDir: String): InstallationSigner

fun keyFingerprint(publicKeyUncompressed: ByteArray): String {
    require(publicKeyUncompressed.size == 65 && publicKeyUncompressed[0] == 0x04.toByte()) {
        "uncompressed P-256 key must be 65 bytes starting with 0x04"
    }
    return publicKeyUncompressed.toByteString().sha256().hex()
}

// DER (platform crypto APIs) <-> raw R||S (our wire format). Pure byte
// math, shared so every platform converts identically.
fun derToRaw(der: ByteArray): ByteArray {
    var pos = 0
    require(der[pos++] == 0x30.toByte()) { "not a DER sequence" }
    val seqLen = derLen(der, pos)
    pos = seqLen.second
    require(der[pos++] == 0x02.toByte()) { "missing R integer" }
    val rLen = derLen(der, pos)
    pos = rLen.second
    val r = der.copyOfRange(pos, pos + rLen.first)
    pos += rLen.first
    require(der[pos++] == 0x02.toByte()) { "missing S integer" }
    val sLen = derLen(der, pos)
    pos = sLen.second
    val s = der.copyOfRange(pos, pos + sLen.first)
    require(pos + sLen.first == der.size) { "trailing bytes" }
    return stripPad(r, 32) + stripPad(s, 32)
}

private fun derLen(der: ByteArray, pos: Int): Pair<Int, Int> {
    val first = der[pos].toInt() and 0xFF
    if (first < 0x80) return first to pos + 1
    val count = first and 0x7F
    require(count in 1..4) { "oversized length" }
    var value = 0
    for (i in 0 until count) value = (value shl 8) or (der[pos + 1 + i].toInt() and 0xFF)
    return value to pos + 1 + count
}

private fun stripPad(v: ByteArray, size: Int): ByteArray {
    var start = 0
    while (start < v.size - 1 && v[start] == 0.toByte()) start++
    val core = v.copyOfRange(start, v.size)
    require(core.size <= size) { "integer overflow" }
    return ByteArray(size - core.size) + core
}

fun rawToDer(raw: ByteArray): ByteArray {
    require(raw.size == 64) { "raw signature must be 64 bytes" }
    val r = encodeInt(raw.copyOfRange(0, 32))
    val s = encodeInt(raw.copyOfRange(32, 64))
    val body = r + s
    return byteArrayOf(0x30.toByte()) + encodeLen(body.size) + body
}

private fun encodeInt(v: ByteArray): ByteArray {
    var start = 0
    while (start < v.size - 1 && v[start] == 0.toByte()) start++
    var core = v.copyOfRange(start, v.size)
    if (core[0].toInt() and 0x80 != 0) core = byteArrayOf(0) + core
    return byteArrayOf(0x02.toByte()) + encodeLen(core.size) + core
}

private fun encodeLen(len: Int): ByteArray =
    if (len < 0x80) {
        byteArrayOf(len.toByte())
    } else {
        val raw = mutableListOf<Byte>()
        var v = len
        while (v > 0) {
            raw.add(0, (v and 0xFF).toByte())
            v = v ushr 8
        }
        byteArrayOf((0x80 or raw.size).toByte()) + raw.toByteArray()
    }
