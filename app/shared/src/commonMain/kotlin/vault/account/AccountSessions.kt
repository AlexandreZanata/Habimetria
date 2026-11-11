package vault.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

fun interface SessionClock { fun millis(): Long }
class MonotonicSessionClock : SessionClock {
    private val start = TimeSource.Monotonic.markNow()
    override fun millis() = start.elapsedNow().inWholeMilliseconds
}

/** Connected authority only; never reads/writes a local collection or queue. */
class AccountSessions(
    internal val api: AccountApi,
    internal val vault: SessionVault,
    private val deviceId: () -> String,
    private val platform: String,
    private val label: String,
    private val clock: SessionClock = MonotonicSessionClock(),
) {
    internal val guard = Mutex()
    private val user = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = user
    internal var snapshot: StoredSession? = null
    private var access: String? = null
    private var deadline = 0L

    suspend fun login(email: String, password: String) = guard.withLock {
        val origin = api.origin
        val previous = vault.read()
        if (previous != null) {
            requireOrigin(previous)
            if (previous.refreshToken != null) throw SessionFailure("already_connected")
            if (!vault.compareAndSet(previous, null)) throw SessionFailure("session_changed")
        }
        val device = deviceId()
        if (!ID.matches(device)) throw SessionFailure("invalid_request")
        val pair = api.login(email, password, device, platform, label)
        val record = pair.stored(origin, device)
        if (!vault.compareAndSet(null, record)) throw SessionFailure("session_changed")
        remember(pair, record)
    }

    suspend fun accessToken(): String = guard.withLock { accessLocked() }

    internal suspend fun accessLocked(): String {
        val saved = vault.read() ?: run { clearMemory(); throw SessionFailure("authentication_required") }
        requireOrigin(saved)
        if (snapshot != null && (snapshot?.userId != saved.userId || snapshot?.sessionId != saved.sessionId)) {
            clearMemory(); throw SessionFailure("session_changed")
        }
        access?.takeIf { clock.millis() < deadline }?.let {
            if (snapshot == saved) return it
            clearMemory(); throw SessionFailure("session_changed")
        }
        val secret = saved.refreshToken ?: throw SessionFailure("sign_in_required")
        val pending = saved.copy(refreshToken = null)
        if (!vault.compareAndSet(saved, pending)) { clearMemory(); throw SessionFailure("session_changed") }
        clearMemory()
        try {
            val pair = api.refresh(secret, saved.deviceId)
            requireScope(pair, saved)
            val next = pair.stored(api.origin, saved.deviceId)
            if (!vault.compareAndSet(pending, next)) throw SessionFailure("session_changed")
            remember(pair, next)
            return pair.access
        } catch (e: CancellationException) { throw e
        } catch (e: SessionFailure) {
            vault.compareAndSet(pending, null)
            throw SessionFailure(if (e.code == "session_changed") e.code else "sign_in_required")
        }
    }

    internal fun requireOrigin(record: StoredSession) {
        if (record.origin != api.origin) throw SessionFailure("origin_mismatch")
    }
    internal fun requireScope(pair: SessionPair, saved: StoredSession) {
        if (pair.id != saved.sessionId || pair.user != saved.userId) throw SessionFailure("invalid_response")
    }
    internal fun remember(pair: SessionPair, record: StoredSession) {
        snapshot = record; access = pair.access
        val duration = pair.seconds * 1000L
        deadline = clock.millis() + duration - minOf(5000L, duration / 10)
        user.value = record.userId
    }
    internal fun clearMemory() { snapshot = null; access = null; deadline = 0; user.value = null }
    internal fun cachedAccess(saved: StoredSession) = access?.takeIf { snapshot == saved && clock.millis() < deadline }

    /** Explicit recovery for corrupt/locked-out stored connection state. */
    suspend fun forget() = guard.withLock { vault.erase(); clearMemory() }
    fun close() { clearMemory(); api.close() }
}
