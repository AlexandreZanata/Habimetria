package vault.account

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Public, neutral account operations; never signs in or touches local work. */
class AccountEnrollment(private val api: AccountApi) {
    suspend fun requestVerification(email: String) =
        submit("registrations", Email(email), 202, "verification_requested")

    suspend fun verify(token: String, password: String) =
        submit("verification", Verification(token, password), 200, "email_verified")

    suspend fun requestRecovery(email: String) =
        submit("recovery", Email(email), 202, "recovery_requested")

    suspend fun resetPassword(token: String, password: String, confirmation: String) =
        submit("password", Password(token, password, confirmation), 200, "password_updated")

    private suspend inline fun <reified T> submit(path: String, payload: T, expected: Int, status: String) {
        val body = Json.encodeToString(payload)
        if (body.encodeToByteArray().size > 4096) throw SessionFailure("invalid_request")
        val response = api.send(HttpMethod.Post, path, body, expected = expected)
        if (api.decode<Reply>(response).status != status) throw SessionFailure("invalid_response")
    }
    @Serializable private data class Email(val email: String) {
        override fun toString() = "[account request redacted]"
    }
    @Serializable private data class Verification(val token: String, val password: String) {
        override fun toString() = "[verification redacted]"
    }
    @Serializable private data class Password(val token: String, val password: String, val password_confirmation: String) {
        override fun toString() = "[password reset redacted]"
    }
    @Serializable private data class Reply(val status: String)
}
