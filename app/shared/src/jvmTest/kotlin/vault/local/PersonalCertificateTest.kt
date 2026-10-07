package vault.local

import java.io.File
import java.nio.file.Files
import kotlin.test.*

internal fun syntheticCertificate(directory: File, bits: Int = 2048, alias: String = "test", startDate: String? = null, validity: Int = 30): File {
    val file = File(directory, "synthetic-$bits.p12")
    val command = listOf(File(System.getProperty("java.home"), "bin/keytool").path, "-genkeypair", "-alias", alias,
        "-keyalg", "RSA", "-keysize", bits.toString(), "-validity", validity.toString(), "-dname", "CN=SYNTHETIC CODE TEST ONLY",
        "-storetype", "PKCS12", "-keystore", file.path, "-storepass", "test-password", "-keypass", "test-password", "-noprompt").toMutableList()
    if (startDate != null) command.addAll(listOf("-startdate", startDate))
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { output }
    return file
}

class PersonalCertificateTest {
    @Test fun holderSignatureIsVerifiedButUnknownAndCredentialsAreCleared() {
        val directory = Files.createTempDirectory("vault-cert-test").toFile()
        try {
            val file = syntheticCertificate(directory)
            val signer = requireNotNull(personalCertificateSigner())
            val password = "test-password".toCharArray()
            val identity = signer.inspect(file.path, password)
            assertTrue(password.all { it == '\u0000' })
            val payload = "synthetic declaration".encodeToByteArray()
            val proof = signer.sign(file.path, "test-password".toCharArray(), payload)
            assertEquals(identity, signer.verify(proof, payload))
            assertNull(signer.verify(proof, "changed".encodeToByteArray()))
            assertNull(signer.verify(proof.copy(chain = proof.chain + proof.chain), payload))
            assertNull(signer.verify(proof.copy(algorithm = "SHA1withRSA"), payload))
            val wrong = "wrong".toCharArray()
            assertFails { signer.inspect(file.path, wrong) }
            assertTrue(wrong.all { it == '\u0000' })
            assertFails { signer.sign(syntheticCertificate(directory, 1024).path, "test-password".toCharArray(), payload) }
            val expiredDir = File(directory, "expired").apply { mkdirs() }
            val expired = syntheticCertificate(expiredDir, startDate = "-2d", validity = 1)
            assertFailsWith<IllegalArgumentException> { signer.sign(expired.path, "test-password".toCharArray(), payload) }
            syntheticCertificate(directory, alias = "second")
            assertFails { signer.inspect(file.path, "test-password".toCharArray()) }
        } finally { directory.deleteRecursively() }
    }
}
