package vault.account

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.request
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Separate, pinned native transport: no cookies, redirects or automatic retries. */
class AccountApi private constructor(
    private val baseUrl: String,
    private val allowLocalHttp: Boolean,
    private val clientFactory: () -> HttpClient,
) {
    constructor(baseUrl: String, engine: HttpClientEngine, allowLocalHttp: Boolean = false) :
        this(baseUrl, allowLocalHttp, { HttpClient(engine) { secureTransport() } })
    constructor(baseUrl: String, engine: HttpClientEngineFactory<*>, allowLocalHttp: Boolean = false) :
        this(baseUrl, allowLocalHttp, { HttpClient(engine) { secureTransport() } })
    private val http = lazy(clientFactory)
    internal val json = Json { ignoreUnknownKeys = true }
    val origin: String get() = try {
        val u = Url(baseUrl)
        val local = u.host in setOf("localhost", "127.0.0.1", "::1", "[::1]")
        if ((u.protocol != URLProtocol.HTTPS && !(allowLocalHttp && local && u.protocol == URLProtocol.HTTP)) ||
            !u.user.isNullOrEmpty() || !u.password.isNullOrEmpty() || u.fragment.isNotEmpty() ||
            !u.parameters.isEmpty() || u.encodedPath !in setOf("", "/")
        ) throw SessionFailure("insecure_origin")
        val host = if (u.host.contains(':') && !u.host.startsWith('[')) "[${u.host}]" else u.host.lowercase()
        "${u.protocol.name}://$host" + if (u.port == u.protocol.defaultPort) "" else ":${u.port}"
    } catch (_: Exception) { throw SessionFailure("insecure_origin") }

    suspend fun login(email: String, password: String, deviceId: String, platform: String, label: String): SessionPair =
        decode(send(HttpMethod.Post, "sessions", json.encodeToString(Login(email, password, Device(deviceId, platform, label)))))
    suspend fun refresh(secret: String, deviceId: String): SessionPair =
        decode(send(HttpMethod.Post, "sessions/refresh", json.encodeToString(Refresh(secret, deviceId))))
    suspend fun devices(access: String): List<DeviceSession> {
        val response = decode<Devices>(send(HttpMethod.Get, "sessions", access = access)).sessions
        if (response.size > 20 || response.any { !ID.matches(it.id) || it.label.length > 160 || it.platform !in setOf("android", "ios") })
            throw SessionFailure("invalid_response")
        return response
    }
    suspend fun profile(access: String): AccountProfile {
        val profile = decode<AccountProfile>(send(HttpMethod.Get, "profile", access = access))
        if (!ID.matches(profile.id) || profile.email.length !in 1..320 || profile.name.length > 320)
            throw SessionFailure("invalid_response")
        return profile
    }
    suspend fun revoke(access: String, id: String) {
        if (!ID.matches(id)) throw SessionFailure("invalid_request")
        send(HttpMethod.Delete, "sessions/$id", access = access, expected = 204)
    }
    fun close() { if (http.isInitialized()) http.value.close() }
    internal inline fun <reified T> decode(text: String): T = try { json.decodeFromString(text) }
        catch (_: Exception) { throw SessionFailure("invalid_response") }

    internal suspend fun send(method: HttpMethod, path: String, body: String? = null, access: String? = null, expected: Int = 200): String {
        val target = origin
        try {
            val response = http.value.request("$target/api/v1/account/$path") {
                this.method = method
                if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
                if (access != null) header(HttpHeaders.Authorization, "Bearer $access")
            }
            if (response.status.value != expected) {
                response.bodyAsChannel().cancel(null)
                throw SessionFailure(when (response.status.value) {
                401 -> "authentication_required"; 409 -> "session_limit"; 429 -> "too_many_attempts"
                400 -> "invalid_request"; 404 -> "not_found"; else -> "service_unavailable"
                })
            }
            val channel = response.bodyAsChannel()
            val bytes = ByteArray(16385)
            var size = 0
            while (size < bytes.size) {
                val count = channel.readAvailable(bytes, size, bytes.size - size)
                if (count < 0) break
                if (count == 0) yield() else size += count
            }
            if (size > 16384) { channel.cancel(null); throw SessionFailure("invalid_response") }
            return bytes.copyOf(size).decodeToString(throwOnInvalidSequence = true)
        } catch (e: CancellationException) { throw e
        } catch (e: SessionFailure) { throw e
        } catch (_: Exception) { throw SessionFailure("network_unavailable") }
    }
    @Serializable private data class Device(val id: String, val platform: String, val label: String)
    @Serializable private data class Login(val email: String, val password: String, val device: Device) {
        override fun toString() = "[login redacted]"
    }
    @Serializable private data class Refresh(val refresh_token: String, val device_id: String) {
        override fun toString() = "[refresh redacted]"
    }
    @Serializable private data class Devices(val sessions: List<DeviceSession>)
}

private fun io.ktor.client.HttpClientConfig<*>.secureTransport() {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 5000; socketTimeoutMillis = 15000 }
}
