package vault

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import vault.api.VaultApi
import vault.rooms.RoomCoverage
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomCoverageTest {

    @Test
    fun percentScalesWithRequiredShots() = runTest {
        assertEquals(100, RoomCoverage("kitchen", emptyList(), emptySet()).percent())
        val empty = RoomCoverage("kitchen", listOf("a", "b", "c", "d"), emptySet())
        assertEquals(0, empty.percent())
        val full = RoomCoverage("kitchen", listOf("a", "b", "c", "d"), setOf("a", "b", "c", "d"))
        assertEquals(100, full.percent())
        val half = RoomCoverage("kitchen", listOf("a", "b", "c", "d"), setOf("a", "b"))
        assertEquals(50, half.percent())
    }
}

class VaultApiTest {

    private fun mockApi(body: String): VaultApi {
        val engine = MockEngine { respond(body, HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json")) }
        return VaultApi("http://test", engine)
    }

    @Test
    fun createPropertyParsesId() = runTest {
        val api = mockApi("""{"id":"p1","address":"Rua X","created_at":"2026-09-25T10:00:00Z"}""")
        assertEquals("p1", api.createProperty("Rua X").id)
    }

    @Test
    fun tenancyWithCreatedAtParses() = runTest {
        val api = mockApi("""{"id":"t1","property_id":"p1","type":"entry","created_at":"2026-09-25T10:00:00Z"}""")
        assertEquals("t1", api.createTenancy("p1").id)
    }

    @Test
    fun sealParsesVerifyCode() = runTest {
        val api = mockApi("""{"inspection_id":"i1","status":"SEALED","manifest_sha256":"m","signature":"s","report_id":"r","verify_code":"ABC123","evidence_count":2}""")
        assertEquals("ABC123", api.seal("i1").verifyCode)
    }

    @Test
    fun createFindingParsesPreset() = runTest {
        val api = mockApi("""{"id":"f1","room_id":"r1","evidence_id":"e1","preset":"crack","note":"hairline","created_at":"2026-09-25T10:00:00Z"}""")
        val finding = api.createFinding("r1", "e1", "crack", "hairline")
        assertEquals("crack", finding.preset)
        assertEquals("hairline", finding.note)
    }

    @Test
    fun listFindingsParsesArray() = runTest {
        val api = mockApi("""[{"id":"f1","room_id":"r1","evidence_id":"e1","preset":"stain","note":null,"created_at":"2026-09-25T10:00:00Z"}]""")
        val findings = api.listFindings("r1")
        assertEquals(1, findings.size)
        assertEquals("stain", findings.single().preset)
    }

    @Test
    fun attachPdfParsesSha() = runTest {
        val api = mockApi("""{"report_id":"r1","inspection_id":"i1","verify_code":"ABC","report_sha256":"deadbeef","pdf_ready":true}""")
        val rep = api.attachPdf("r1", "%PDF-1.4".encodeToByteArray())
        assertEquals("deadbeef", rep.reportSha256)
    }

    @Test
    fun verifyParsesAuthentic() = runTest {
        val api = mockApi("""{"authentic":true,"verify_code":"ABC","sealed_at":"2026-09-25T14:00:00Z","evidence_count":2,"manifest_sha256":"m","signature_valid":true,"report_sha256":"r","pdf_ready":true}""")
        val out = api.verify("ABC")
        assertEquals(true, out.authentic)
        assertEquals(true, out.signatureValid)
        assertEquals(2, out.evidenceCount)
    }

    @Test
    fun evidencesParseInSequence() = runTest {
        val api = mockApi("""[{"id":"e2","sequence":2,"sha256":"bb","mime":"image/jpeg","size":200,"room_id":"r1","received_at_server":"2026-09-25T10:00:00Z"},{"id":"e1","sequence":1,"sha256":"aa","mime":"image/jpeg","size":100,"room_id":null,"received_at_server":"2026-09-25T10:00:00Z"}]""")
        val lines = api.evidences("i1")
        assertEquals(2, lines.size)
        assertEquals("r1", lines[0].roomId)
    }

    @Test
    fun groupEvidencesByRoom() {
        val rooms = listOf(
            vault.api.Room("r1", "i1", "kitchen", "Kitchen", 1),
            vault.api.Room("r2", "i1", "living", "Living", 2),
        )
        val lines = listOf(
            vault.api.EvidenceLine("e1", 1, "aa", "image/jpeg", 100, "r1", ""),
            vault.api.EvidenceLine("e2", 2, "bb", "image/jpeg", 100, null, ""),
        )
        val groups = vault.api.groupEvidences(rooms, lines)
        assertEquals(1, groups[0].lines.size)
        assertEquals(0, groups[1].lines.size)
    }
}
