package vault.local

import java.io.File
import java.security.AlgorithmParameters
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString

actual fun personalCertificateSigner(): PersonalCertificateSigner? = JavaPersonalCertificateSigner()

private class JavaPersonalCertificateSigner : PersonalCertificateSigner {
    override fun inspect(path: String, password: CharArray): CertificateIdentity = load(path, password).second.first().identity()

    override fun sign(path: String, password: CharArray, payload: ByteArray): CertificateProof {
        val (key, chain) = load(path, password)
        val algorithm = algorithm(chain.first())
        val signature = Signature.getInstance(algorithm).run { initSign(key); update(payload); sign() }
        val proof = CertificateProof(chain.map { it.encoded.toByteString().hex() }, algorithm, signature.toByteString().hex())
        require(verify(proof, payload) != null) { "certificate private key mismatch" }
        return proof
    }

    override fun verify(proof: CertificateProof, payload: ByteArray): CertificateIdentity? = runCatching {
        require(proof.chain.size in 1..8 && proof.chain.distinct().size == proof.chain.size)
        val factory = CertificateFactory.getInstance("X.509")
        val chain = proof.chain.map { hex ->
            require(hex.length in 2..32768)
            val bytes = hex.decodeHex().toByteArray()
            (factory.generateCertificate(bytes.inputStream()) as X509Certificate).also { require(it.encoded.contentEquals(bytes)) }
        }
        chain.zipWithNext().forEach { (child, parent) ->
            require(parent.basicConstraints >= 0 && parent.keyUsage?.getOrNull(5) != false && child.issuerX500Principal == parent.subjectX500Principal)
            child.verify(parent.publicKey)
        }
        val leaf = chain.first()
        require(proof.algorithm == algorithm(leaf) && leaf.basicConstraints < 0)
        require(leaf.keyUsage?.getOrNull(0) != false) { "certificate not usable for signing" }
        val valid = Signature.getInstance(proof.algorithm).run {
            initVerify(leaf.publicKey); update(payload); verify(proof.signature.decodeHex().toByteArray())
        }
        require(valid) { "invalid personal signature" }
        leaf.identity()
    }.getOrNull()

    private fun load(path: String, password: CharArray): Pair<PrivateKey, List<X509Certificate>> {
        try {
            val file = File(path)
            require(file.isFile && file.length() in 1..1_048_576) { "invalid A1 file size" }
            val store = KeyStore.getInstance("PKCS12")
            try { file.inputStream().use { store.load(it, password) } }
            catch (e: Exception) { throw IllegalArgumentException("A1 file or password invalid", e) }
            val aliases = store.aliases().toList().filter(store::isKeyEntry)
            require(aliases.size == 1) { "select a PKCS12 with exactly one private key" }
            val key = store.getKey(aliases.single(), password) as? PrivateKey ?: error("private key unavailable")
            val chain = store.getCertificateChain(aliases.single()).map { it as X509Certificate }
            require(chain.size in 1..8 && chain.first().basicConstraints < 0) { "end-entity certificate required" }
            try { chain.first().checkValidity() } // Device-clock check only; no ACT/CRL claim.
            catch (e: java.security.cert.CertificateException) { throw IllegalArgumentException("certificate outside device validity period", e) }
            algorithm(chain.first())
            return key to chain
        } finally { password.fill('\u0000') }
    }

    private fun algorithm(cert: X509Certificate): String = when (val key = cert.publicKey) {
        is RSAPublicKey -> { require(key.modulus.bitLength() in 2048..8192) { "RSA key too weak or too large" }; "SHA256withRSA" }
        is ECPublicKey -> {
            val expected = AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec("secp256r1")) }.getParameterSpec(ECParameterSpec::class.java)
            require(key.params.order == expected.order && key.params.curve == expected.curve && key.params.generator == expected.generator && key.params.cofactor == expected.cofactor) { "only P-256 supported" }
            "SHA256withECDSA"
        }
        else -> error("unsupported certificate key")
    }

    private fun X509Certificate.identity() = CertificateIdentity(subjectX500Principal.name, issuerX500Principal.name,
        encoded.toByteString().sha256().hex(), serialNumber.toString(16), notBefore.toInstant().toString(), notAfter.toInstant().toString())
}
