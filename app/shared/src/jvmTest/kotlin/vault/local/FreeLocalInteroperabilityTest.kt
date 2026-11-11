package vault.local

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreeLocalInteroperabilityTest {
    @Test fun exportedBytesVerifyWithoutSourceDatabaseOrPrivateKey() = runTest {
        val fixture = FreeLocalFixture(FreeLocalAccess.EXPIRED_HOSTED_ACCESS)
        val recipient = Files.createTempDirectory("free-local-recipient").toFile()
        val archive = File(recipient, "report.zip")
        try {
            try {
                val graph = fixture.seed()
                fixture.sealer().seal(graph.inspectionId)
                val report = fixture.generator().generate(graph.inspectionId, FreeLocalPlanRenderer())
                ReportDeclarations(fixture.session.db, fixture.directory, fixture.signer)
                    .append(report.id, DeclarationIntent.AUTHOR, "", "", "", true)
                File(fixture.generator().exportZip(report.id)).copyTo(archive)
                fixture.network.assertNoTraffic(fixture.session, graph)
            } finally { fixture.close() }
            // The issuer DB, originals and installation private key are gone.
            val imported = importPackageZip(archive.path, recipient.path)
            val verdict = independentPackageVerdict(imported)
            assertTrue(verdict.getValue("accepted").jsonPrimitive.boolean, verdict.toString())
            assertEquals(17, verdict.getValue("evidencesChecked").jsonPrimitive.content.toInt())
            assertEquals("UNKNOWN", verdict.getValue("issuerTrust").jsonPrimitive.content)
            assertEquals(1, verdict.getValue("declarations").jsonArray.size)
            val pdf = File(imported, "report.pdf")
            val bytes = pdf.readBytes()
            bytes[0] = (bytes[0].toInt() xor 1).toByte()
            pdf.writeBytes(bytes)
            assertFalse(independentPackageVerdict(imported).getValue("accepted").jsonPrimitive.boolean)
        } finally { recipient.deleteRecursively() }
    }
}
