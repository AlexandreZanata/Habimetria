package vault.account

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SessionTransportTest {
    @Test fun unsafeOriginsNeverReceiveCredentials() = runTest {
        for (origin in listOf("http://remote.example.test", "https://user:secret@example.test", "$ORIGIN/path", "$ORIGIN?query=1", "$ORIGIN#fragment")) {
            var calls = 0
            val api = AccountApi(origin, MockEngine { calls++; respond("{}") })
            assertEquals("insecure_origin", assertFailsWith<SessionFailure> { api.login("user@example.test", "secret", DEVICE, "android", "phone") }.code)
            assertEquals(0, calls); api.close()
        }
    }
    @Test fun redirectsAreNotFollowed() = runTest {
        var calls = 0
        val api = AccountApi(ORIGIN, MockEngine { calls++; respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://other.example.test/steal")) })
        assertFailsWith<SessionFailure> { api.login("user@example.test", "secret", DEVICE, "android", "phone") }
        assertEquals(1, calls); api.close()
    }
    @Test fun originMismatchPreservesOtherServerRecord() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; respondPair() }
        val other = pair().stored("https://other.example.test", DEVICE)
        f.vault.record = other
        assertEquals("origin_mismatch", assertFailsWith<SessionFailure> { f.sessions.accessToken() }.code)
        assertEquals(other, f.vault.record); assertEquals(0, calls)
        f.sessions.close()
    }
    @Test fun boundResponseAndStorageRejectCorruption() = runTest {
        val api = AccountApi(ORIGIN, MockEngine { respond("x".repeat(17000)) })
        assertEquals("invalid_response", assertFailsWith<SessionFailure> { api.login("user@example.test", "secret", DEVICE, "android", "phone") }.code)
        api.close()
        assertFailsWith<SessionFailure> { SessionEncoding.decode("not-json".encodeToByteArray()) }
        assertFalse(pair().toString().contains(pair().refresh))
        val record = pair().stored(ORIGIN, DEVICE)
        assertEquals(record, SessionEncoding.decode(SessionEncoding.encode(record)))
        assertFalse(record.toString().contains(record.refreshToken!!))
    }
}
