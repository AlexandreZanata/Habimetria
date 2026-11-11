package vault.account

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SessionRotationTest {
    @Test fun concurrentAccessUsesOneDurableRotation() = runTest {
        lateinit var f: SessionFixture
        var refreshes = 0
        f = SessionFixture { request ->
            assertNull(request.headers["Authorization"])
            if (request.url.encodedPath.endsWith("/refresh")) {
                refreshes++
                assertNull(f.vault.record?.refreshToken)
                respondPair(1)
            } else respondPair()
        }
        f.sessions.login("user@example.test", "fixture password")
        assertEquals(pair().access, f.sessions.accessToken())
        assertEquals(pair().refresh, f.vault.record?.refreshToken)
        f.clock.now = 900_000
        val tokens = List(10) { async { f.sessions.accessToken() } }.awaitAll()
        assertTrue(tokens.all { it == pair(1).access })
        assertEquals(1, refreshes)
        assertEquals(pair(1).refresh, f.vault.record?.refreshToken)
        assertEquals(USER, f.sessions.currentUser.value)
        f.sessions.close()
    }

    @Test fun lostResponseCannotReplayPersistedSecret() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; throw IllegalStateException("sensitive transport detail") }
        f.vault.record = pair().stored(ORIGIN, DEVICE)
        val failure = assertFailsWith<SessionFailure> { f.sessions.accessToken() }
        assertEquals("sign_in_required", failure.code)
        assertNull(f.vault.record)
        assertFailsWith<SessionFailure> { f.sessions.accessToken() }
        assertEquals(1, calls)
        f.sessions.close()
    }

    @Test fun pendingCrashRequiresNewLoginWithoutRefresh() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; respondPair() }
        f.vault.record = pair().stored(ORIGIN, DEVICE).copy(refreshToken = null)
        assertEquals("sign_in_required", assertFailsWith<SessionFailure> { f.sessions.accessToken() }.code)
        assertEquals(0, calls)
        f.sessions.login("user@example.test", "fixture password")
        assertEquals(1, calls)
        f.sessions.close()
    }

    @Test fun unavailablePersistenceDoesNotSendOldSecret() = runTest {
        var calls = 0
        val f = SessionFixture { calls++; respondPair(1) }
        f.vault.record = pair().stored(ORIGIN, DEVICE)
        f.vault.failPending = true
        assertEquals("storage_unavailable", assertFailsWith<SessionFailure> { f.sessions.accessToken() }.code)
        assertEquals(0, calls)
        assertEquals(pair().refresh, f.vault.record?.refreshToken)
        f.vault.failPending = false; f.vault.failActive = true
        assertFailsWith<SessionFailure> { f.sessions.accessToken() }
        assertNull(f.vault.record)
        assertEquals(1, calls)
        f.sessions.close()
    }

    @Test fun changedResponseScopeIsRejected() = runTest {
        val f = SessionFixture { respondPair() }
        f.vault.record = pair().stored(ORIGIN, DEVICE).copy(userId = "44444444-4444-4444-8444-444444444444")
        assertFailsWith<SessionFailure> { f.sessions.accessToken() }
        assertNull(f.vault.record)
        f.sessions.close()
    }
}
