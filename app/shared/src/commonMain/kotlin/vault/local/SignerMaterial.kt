package vault.local

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath

// Public material only; retention is not an identity/trust endorsement.
class SignerMaterial(private val filesDir: String, private val fs: FileSystem = FileSystem.SYSTEM) {
    fun retain(signer: InstallationSigner) {
        val bytes = signer.publicKeyUncompressed()
        require(keyFingerprint(bytes) == signer.keyId) { "signer key identity mismatch" }
        val path = path(signer.keyId)
        if (fs.exists(path)) {
            require(fs.read(path) { readUtf8() } == bytes.toByteString().hex()) { "historical key changed" }
            return
        }
        fs.createDirectories(path.parent!!)
        val tmp = path.parent!!.resolve("${path.name}.tmp")
        fs.write(tmp) { writeUtf8(bytes.toByteString().hex()) }
        fs.atomicMove(tmp, path)
    }

    fun key(keyId: String, current: InstallationSigner): ByteArray {
        if (keyId == current.keyId) {
            retain(current)
            return current.publicKeyUncompressed()
        }
        val path = path(keyId)
        require(fs.exists(path)) { "historical public key unavailable; import the original package" }
        val bytes = fs.read(path) { readUtf8() }.decodeHex().toByteArray()
        require(keyFingerprint(bytes) == keyId) { "historical public key mismatch" }
        return bytes
    }

    private fun path(keyId: String): okio.Path {
        require(keyId.matches(Regex("[0-9a-f]{64}"))) { "invalid key identity" }
        return "$filesDir/vault/public_keys/$keyId.hex".toPath()
    }
}
