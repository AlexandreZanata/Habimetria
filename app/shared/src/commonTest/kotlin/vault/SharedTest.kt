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
    fun sealParsesVerifyCode() = runTest {
        val api = mockApi("""{"inspection_id":"i1","status":"SEALED","manifest_sha256":"m","signature":"s","report_id":"r","verify_code":"ABC123","evidence_count":2}""")
        assertEquals("ABC123", api.seal("i1").verifyCode)
    }

    @Test
    fun findingParsesPreset() = runTest {
        val api = mockApi("""{"id":"f1","room_id":"r1","evidence_id":"e1","preset":"crack","note":"n"}""")
        assertEquals("crack", api.createFinding("r1", "e1", "crack", "n").preset)
    }
}
