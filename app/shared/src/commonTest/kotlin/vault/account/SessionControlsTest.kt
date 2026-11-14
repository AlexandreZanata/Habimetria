package vault.account

import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SessionControlsTest {
    @Test fun savedSessionPresenceHonorsOriginWithoutNetwork() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; respondPair() }
        assertFalse(f.sessions.hasSavedSession())
        f.vault.record = pair().stored(ORIGIN, DEVICE)
        assertTrue(f.sessions.hasSavedSession())
        f.vault.record = pair().stored("https://other.example.test", DEVICE)
        assertEquals("origin_mismatch", assertFailsWith<SessionFailure> { f.sessions.hasSavedSession() }.code)
        assertEquals(0, calls)
        f.sessions.close()
    }
    @Test fun staleCoordinatorCannotUseOrLogoutAnotherAccount() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; respondPair() }
        f.sessions.login("user@example.test", "fixture password")
        val other = pair().stored(ORIGIN, DEVICE).copy(userId = "44444444-4444-4444-8444-444444444444")
        f.vault.record = other
        assertEquals("session_changed", assertFailsWith<SessionFailure> { f.sessions.logout() }.code)
        assertEquals(other, f.vault.record); assertEquals(1, calls)
        f.sessions.close()
        val second = SessionFixture { calls++; respondPair() }
        second.sessions.login("user@example.test", "fixture password")
        second.vault.record = other
        assertEquals("session_changed", assertFailsWith<SessionFailure> { second.sessions.accessToken() }.code)
        assertEquals(other, second.vault.record); assertEquals(2, calls)
        second.sessions.close()
    }
    @Test fun logoutForgetsBeforeRemoteRequestAndReportsOffline() = runTest {
        lateinit var f: SessionFixture
        var deleted = false
        f = SessionFixture { request ->
            if (request.method == HttpMethod.Delete) {
                assertNull(f.vault.record)
                assertEquals("Bearer ${pair().access}", request.headers[HttpHeaders.Authorization])
                deleted = true; respond("", HttpStatusCode.NoContent)
            } else respondPair()
        }
        f.sessions.login("user@example.test", "fixture password")
        assertEquals(LogoutResult(true, true), f.sessions.logout())
        assertTrue(deleted); assertNull(f.sessions.currentUser.value); f.sessions.close()
        val offline = SessionFixture { throw IllegalStateException("offline") }
        offline.vault.record = pair().stored(ORIGIN, DEVICE)
        assertEquals(LogoutResult(true, false), offline.sessions.logout())
        assertNull(offline.vault.record); offline.sessions.close()
    }
    @Test fun revokedAccountClearsOnlyItsSavedGeneration() = runTest {
        var login = true
        val f = SessionFixture {
            if (login) { login = false; respondPair() }
            else respond("", HttpStatusCode.Unauthorized)
        }
        f.sessions.login("user@example.test", "fixture password")
        assertFailsWith<SessionFailure> { f.sessions.devices() }
        assertNull(f.vault.record); assertNull(f.sessions.currentUser.value)
        f.sessions.forget(); f.sessions.close()
    }
}
