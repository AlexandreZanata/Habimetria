package vault.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SessionFailure(val code: String) : Exception(code)

@Serializable
data class StoredSession(
    val origin: String,
    val deviceId: String,
    val userId: String,
    val sessionId: String,
    val refreshToken: String?,
    val refreshExpiresAt: String,
) {
    override fun toString() = "[stored account credentials redacted]"
}

@Serializable
data class SessionPair(
    @SerialName("access_token") val access: String,
    @SerialName("refresh_token") val refresh: String,
    @SerialName("session_id") val id: String,
    @SerialName("user_id") val user: String,
    @SerialName("token_type") val kind: String,
    @SerialName("expires_in") val seconds: Int,
    @SerialName("refresh_expires_at") val refreshExpiresAt: String,
) {
    override fun toString() = "[account response credentials redacted]"
    internal fun stored(origin: String, device: String): StoredSession {
        if (!ACCESS.matches(access) || !REFRESH.matches(refresh) || !ID.matches(id) ||
            !ID.matches(user) || kind != "Bearer" || seconds !in 1..900 || refreshExpiresAt.length !in 1..80
        ) throw SessionFailure("invalid_response")
        return StoredSession(origin, device, user, id, refresh, refreshExpiresAt)
    }
}

@Serializable
data class DeviceSession(
    @SerialName("session_id") val id: String,
    val platform: String,
    val label: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_used_at") val lastUsedAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

data class LogoutResult(val localForgotten: Boolean, val remoteRevoked: Boolean)
internal val ID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
internal val ACCESS = Regex("[A-Za-z0-9_-]{43}")
internal val REFRESH = Regex("hr1_[A-Za-z0-9_-]{43}")
