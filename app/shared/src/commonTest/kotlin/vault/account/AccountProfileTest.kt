package vault.account

import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AccountProfileTest {
    @Test fun profileUsesBearerAndRejectsAnotherUser() = runTest {
        var other = false
        val f = SessionFixture { request ->
            if (request.url.encodedPath.endsWith("/profile")) {
                assertEquals("Bearer ${pair().access}", request.headers[HttpHeaders.Authorization])
                val id = if (other) "44444444-4444-4444-8444-444444444444" else USER
                respond("""{"id":"$id","email":"user@example.test","name":"Example"}""", HttpStatusCode.OK)
            } else respondPair()
        }
        f.sessions.login("user@example.test", "synthetic-password")
        assertEquals(USER, f.sessions.profile().id)
        other = true
        assertEquals("invalid_response", assertFailsWith<SessionFailure> { f.sessions.profile() }.code)
        assertNotNull(f.vault.record)
        f.sessions.close()
    }
    @Test fun unauthorizedProfileForgetsOnlyMatchingSession() = runTest {
        val f = SessionFixture { request ->
            if (request.url.encodedPath.endsWith("/profile")) respond("", HttpStatusCode.Unauthorized) else respondPair()
        }
        f.sessions.login("user@example.test", "synthetic-password")
        assertEquals("authentication_required", assertFailsWith<SessionFailure> { f.sessions.profile() }.code)
        assertNull(f.vault.record)
        assertNull(f.sessions.currentUser.value)
        f.sessions.close()
    }
}
