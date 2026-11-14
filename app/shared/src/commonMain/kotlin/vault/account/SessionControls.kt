package vault.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock

suspend fun AccountSessions.profile(): AccountProfile = guard.withLock {
    val access = accessLocked()
    val current = snapshot
    try {
        val profile = api.profile(access)
        if (profile.id != current?.userId) throw SessionFailure("invalid_response")
        profile
    } catch (e: SessionFailure) {
        if (e.code == "authentication_required") { if (current != null) vault.compareAndSet(current, null); clearMemory() }
        throw e
    }
}

suspend fun AccountSessions.devices(): List<DeviceSession> = guard.withLock {
    val access = accessLocked()
    val current = snapshot
    try { api.devices(access) } catch (e: SessionFailure) {
        if (e.code == "authentication_required") { if (current != null) vault.compareAndSet(current, null); clearMemory() }
        throw e
    }
}

suspend fun AccountSessions.revokeDevice(id: String) = guard.withLock {
    val access = accessLocked()
    val current = snapshot
    try { api.revoke(access, id) } catch (e: SessionFailure) {
        if (e.code == "authentication_required") { if (current != null) vault.compareAndSet(current, null); clearMemory() }
        throw e
    }
    if (current?.sessionId == id) { vault.compareAndSet(current, null); clearMemory() }
}

suspend fun AccountSessions.logout(): LogoutResult = guard.withLock {
    val saved = vault.read()
    if (saved == null) { clearMemory(); return@withLock LogoutResult(true, false) }
    requireOrigin(saved)
    if (snapshot != null && (snapshot?.userId != saved.userId || snapshot?.sessionId != saved.sessionId)) {
        clearMemory(); throw SessionFailure("session_changed")
    }
    val cached = cachedAccess(saved)
    if (!vault.compareAndSet(saved, null)) throw SessionFailure("session_changed")
    clearMemory()
    val revoked = try {
        val access = cached ?: saved.refreshToken?.let { secret ->
            api.refresh(secret, saved.deviceId).also { requireScope(it, saved); it.stored(api.origin, saved.deviceId) }.access
        }
        if (access == null) false else { api.revoke(access, saved.sessionId); true }
    } catch (e: CancellationException) { throw e
    } catch (_: SessionFailure) { false }
    LogoutResult(true, revoked)
}
