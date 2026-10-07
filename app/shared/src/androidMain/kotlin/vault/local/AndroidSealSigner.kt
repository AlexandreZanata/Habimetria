package vault.local

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

// Keystore-backed installation key: StrongBox when present, TEE
// otherwise (detected, never assumed). No key material ever leaves the
// store; signatures are ECDSA P-256/SHA-256 converted to raw R||S.
actual fun platformSigner(filesDir: String): InstallationSigner = AndroidInstallationSigner()

class AndroidInstallationSigner : InstallationSigner {
    private val alias = "vault-seal-1"
    private val entry: KeyStore.PrivateKeyEntry = loadOrCreate()
    override val hardwareBacked: Boolean = detectHardware(entry)
    override val validationOnly: Boolean = !hardwareBacked

    override val keyId: String get() = keyFingerprint(publicKeyUncompressed())

    override fun publicKeyUncompressed(): ByteArray {
        val point = (entry.certificate.publicKey as ECPublicKey).w
        return byteArrayOf(0x04.toByte()) + toFixed(point.affineX, 32) + toFixed(point.affineY, 32)
    }

    override fun sign(bytes: ByteArray): ByteArray {
        val der = Signature.getInstance("SHA256withECDSA").apply {
            initSign(entry.privateKey)
            update(bytes)
        }.sign()
        return derToRaw(der)
    }

    override fun verify(publicKeyUncompressed: ByteArray, bytes: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false
        return try {
            keyFingerprint(publicKeyUncompressed)
            val point = ECPoint(
                BigInteger(1, publicKeyUncompressed.copyOfRange(1, 33)),
                BigInteger(1, publicKeyUncompressed.copyOfRange(33, 65)),
            )
            val pub = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, ecParams()))
            Signature.getInstance("SHA256withECDSA").apply {
                initVerify(pub)
                update(bytes)
            }.verify(rawToDer(signature))
        } catch (_: Exception) {
            false
        }
    }

    private fun loadOrCreate(): KeyStore.PrivateKeyEntry {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(alias, null) as? KeyStore.PrivateKeyEntry)?.let { return it }
        if (Build.VERSION.SDK_INT >= 28) {
            try { generate(strongbox = true) }
            catch (_: StrongBoxUnavailableException) { generate(strongbox = false) }
        } else generate(strongbox = false)
        return store.getEntry(alias, null) as KeyStore.PrivateKeyEntry
    }

    private fun generate(strongbox: Boolean) {
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(false)
        if (Build.VERSION.SDK_INT >= 28) builder.setIsStrongBoxBacked(strongbox)
        val spec = builder.build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(spec)
        }.generateKeyPair()
    }

    private fun detectHardware(entry: KeyStore.PrivateKeyEntry): Boolean =
        try {
            val info = KeyFactory.getInstance(entry.privateKey.algorithm, "AndroidKeyStore")
                .getKeySpec(entry.privateKey, KeyInfo::class.java) as KeyInfo
            if (Build.VERSION.SDK_INT >= 31) info.securityLevel != KeyProperties.SECURITY_LEVEL_SOFTWARE else info.isInsideSecureHardware
        } catch (_: Exception) {
            false
        }

    private fun ecParams(): ECParameterSpec =
        AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)

    private fun toFixed(v: BigInteger, size: Int): ByteArray {
        val raw = v.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        require(raw.size <= size) { "scalar overflow" }
        return ByteArray(size - raw.size) + raw
    }
}
