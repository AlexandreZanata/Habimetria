package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

// Validation-only software key for JVM/desktop and tests. Key material
// lives in plain files under filesDir: it proves format and protocol,
// never production trust.
actual fun platformSigner(filesDir: String): InstallationSigner = JvmInstallationSigner(filesDir)

class JvmInstallationSigner(filesDir: String) : InstallationSigner {
    private val privateFile = "$filesDir/vault/seal-key.pk8".toPath()
    private val publicFile = "$filesDir/vault/seal-key.pub".toPath()
    private val privateKey: java.security.PrivateKey
    private val publicBytes: ByteArray

    init {
        val fs = FileSystem.SYSTEM
        if (fs.exists(privateFile) && fs.exists(publicFile)) {
            val factory = KeyFactory.getInstance("EC")
            privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(fs.read(privateFile) { readByteArray() }))
            publicBytes = fs.read(publicFile) { readByteArray() }
            // Stored public material must match the private key: prove it
            // with a probe signature instead of trusting the files.
            val probe = "vault-key-check".encodeToByteArray()
            require(verify(publicBytes, probe, signWith(privateKey, probe))) {
                "stored key mismatch"
            }
        } else {
            val pair = KeyPairGenerator.getInstance("EC").apply {
                initialize(ECGenParameterSpec("secp256r1"))
            }.generateKeyPair()
            privateKey = pair.private
            val point = (pair.public as ECPublicKey).w
            publicBytes = byteArrayOf(0x04.toByte()) + toFixed(point.affineX, 32) + toFixed(point.affineY, 32)
            fs.createDirectories(privateFile.parent!!)
            fs.write(privateFile) { write(pair.private.encoded) }
            fs.write(publicFile) { write(publicBytes) }
        }
    }

    override val keyId: String get() = keyFingerprint(publicBytes)
    override val hardwareBacked: Boolean = false
    override val validationOnly: Boolean = true

    override fun publicKeyUncompressed(): ByteArray = publicBytes.copyOf()

    override fun sign(bytes: ByteArray): ByteArray = signWith(privateKey, bytes)

    private fun signWith(key: java.security.PrivateKey, bytes: ByteArray): ByteArray {
        val der = Signature.getInstance("SHA256withECDSA").apply {
            initSign(key)
            update(bytes)
        }.sign()
        return derToRaw(der)
    }

    override fun verify(publicKeyUncompressed: ByteArray, bytes: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false
        return try {
            val pub = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(publicX509(publicKeyUncompressed)))
            Signature.getInstance("SHA256withECDSA").apply {
                initVerify(pub)
                update(bytes)
            }.verify(rawToDer(signature))
        } catch (_: Exception) {
            false
        }
    }

    // Minimal X.509 SubjectPublicKeyInfo wrapper for a P-256 uncompressed
    // point (fixed 26-byte header for secp256r1 + 65-byte point).
    private fun publicX509(uncompressed: ByteArray): ByteArray {
        keyFingerprint(uncompressed) // validates shape eagerly
        val header = byteArrayOf(
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02,
            0x01, 0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07,
            0x03, 0x42, 0x00,
        )
        return header + uncompressed
    }

    private fun toFixed(v: BigInteger, size: Int): ByteArray {
        val raw = v.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        require(raw.size <= size) { "scalar overflow" }
        return ByteArray(size - raw.size) + raw
    }
}
