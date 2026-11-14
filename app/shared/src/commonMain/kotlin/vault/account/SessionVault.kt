package vault.account

import kotlinx.serialization.json.Json

/** Implementations must persist compare-and-set before returning success. */
interface SessionVault {
    suspend fun read(): StoredSession?
    suspend fun compareAndSet(expected: StoredSession?, updated: StoredSession?): Boolean
    /** Explicitly forget even corrupt/unreadable connected credentials only. */
    suspend fun erase()
}

object SessionEncoding {
    private val json = Json { ignoreUnknownKeys = false }
    fun encode(record: StoredSession): ByteArray {
        validate(record)
        return json.encodeToString(record).encodeToByteArray()
    }
    fun decode(bytes: ByteArray): StoredSession {
        if (bytes.size !in 1..8192) throw SessionFailure("storage_unavailable")
        return try {
            json.decodeFromString<StoredSession>(bytes.decodeToString(throwOnInvalidSequence = true)).also(::validate)
        } catch (_: Exception) { throw SessionFailure("storage_unavailable") }
    }
    private fun validate(r: StoredSession) {
        if (r.origin.length !in 1..512 || !ID.matches(r.deviceId) || !ID.matches(r.userId) ||
            !ID.matches(r.sessionId) || (r.refreshToken != null && !REFRESH.matches(r.refreshToken)) ||
            r.refreshExpiresAt.length !in 1..80
        ) throw SessionFailure("storage_unavailable")
    }
}
