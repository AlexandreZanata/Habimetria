package vault.account

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal const val USER = "11111111-1111-4111-8111-111111111111"
internal const val FAMILY = "22222222-2222-4222-8222-222222222222"
internal const val DEVICE = "33333333-3333-4333-8333-333333333333"
internal const val ORIGIN = "https://account.example.test"
internal fun pair(n: Int = 0) = SessionPair(('A' + n).toString().repeat(42) + "A", "hr1_" + ('D' + n).toString().repeat(42) + "A", FAMILY, USER, "Bearer", 900, "2026-12-11T12:00:00Z")

/** Test-only adapter. Production supplies Keystore/Keychain implementations. */
internal class MemoryVault : SessionVault {
    var record: StoredSession? = null
    var failPending = false
    var failActive = false
    private val guard = Mutex()
    override suspend fun read() = guard.withLock { record }
    override suspend fun compareAndSet(expected: StoredSession?, updated: StoredSession?) = guard.withLock {
        if (record != expected) return@withLock false
        if (updated != null && ((updated.refreshToken == null && failPending) || (updated.refreshToken != null && failActive))) throw SessionFailure("storage_unavailable")
        record = updated; true
    }
    override suspend fun erase() { guard.withLock { record = null } }
}
internal class TestClock : SessionClock { var now = 0L; override fun millis() = now }
internal class SessionFixture(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val vault = MemoryVault()
    val clock = TestClock()
    val api = AccountApi(ORIGIN, MockEngine(handler))
    val sessions = AccountSessions(api, vault, { DEVICE }, "android", "Fixture phone", clock)
}
internal fun MockRequestHandleScope.respondPair(n: Int = 0) = respond(Json.encodeToString(pair(n)), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
