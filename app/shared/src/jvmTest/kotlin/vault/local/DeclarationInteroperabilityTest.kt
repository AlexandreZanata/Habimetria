package vault.local

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DeclarationInteroperabilityTest {
    @Test fun independentVerifierAcceptsAppCertificateAndRejectsMissingDeclaration() = runTest {
        val (db, session, signer) = sealedHarness(1)
        val directory = Files.createTempDirectory("vault-cert-package").toFile()
        try {
            val file = syntheticCertificate(directory)
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            val declared = ReportDeclarations(db, session.filesDir, signer).append(report.id, DeclarationIntent.AUTHOR,
                "Declared author", "engineer", "Code test only", true, file.path, "test-password".toCharArray())
            assertNotNull(declared.certificate)
            assertFalse(declared.trusted)
            val exported = generator.exportPackage(report.id)
            val verdict = PackageVerifier(signer).verify(exported)
            assertTrue(verdict.accepted())
            assertFalse(verdict.declarations.single().trusted)
            assertTrue(PackageVerifier(signer).verify(exported, setOf(declared.keyId)).declarations.single().trusted)
            val go = independentVerdict(exported)
            assertTrue(go.getValue("accepted").jsonPrimitive.boolean, go.toString())
            assertEquals("UNKNOWN", go.getValue("declarations").jsonArray.single().jsonObject.getValue("trust").jsonPrimitive.content)
            File(exported, "declarations").listFiles()!!.single().delete()
            assertFalse(independentVerdict(exported).getValue("accepted").jsonPrimitive.boolean)
        } finally { directory.deleteRecursively(); session.close() }
    }

    private fun independentVerdict(directory: String): JsonObject {
        var root = File(System.getProperty("user.dir")).absoluteFile
        while (!File(root, "backend/go.mod").isFile) root = root.parentFile ?: error("repository not found")
        val process = ProcessBuilder("go", "run", "./cmd/verify", "-format", "json", directory)
            .directory(File(root, "backend")).start()
        // Drain stderr separately: `go run` may emit toolchain or module
        // progress there (cold cache on CI). Merging it into stdout corrupts
        // JSON parsing, so only stdout is parsed below.
        val stderr = StringBuilder()
        val drain = Thread {
            process.errorStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    stderr.appendLine(line)
                    line = reader.readLine()
                }
            }
        }
        drain.start()
        val output = process.inputStream.bufferedReader().readText()
        drain.join(30_000)
        val exit = process.waitFor()
        require(exit == 0 || exit == 1) { "verifier exit=$exit stderr=$stderr" }
        // go run appends "exit status 1" on a rejected package; isolate JSON output.
        return try {
            Json.parseToJsonElement(output.substringBeforeLast('}') + "}").jsonObject
        } catch (e: Exception) {
            error("unparsable verifier stdout (exit=$exit stderr=$stderr): $output")
        }
    }
}
