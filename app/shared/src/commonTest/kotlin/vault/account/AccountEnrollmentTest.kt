package vault.account

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AccountEnrollmentTest {
    @Test fun publicFlowsRequireTheirExplicitAcknowledgments() = runTest {
        val calls = mutableListOf<String>()
        val api = AccountApi(ORIGIN, MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            val path = request.url.encodedPath.substringAfterLast('/')
            calls.add(path)
            val (code, status) = when (path) {
                "registrations" -> HttpStatusCode.Accepted to "verification_requested"
                "verification" -> HttpStatusCode.OK to "email_verified"
                "recovery" -> HttpStatusCode.Accepted to "recovery_requested"
                "password" -> HttpStatusCode.OK to "password_updated"
                else -> error("unexpected path")
            }
            respond("""{"status":"$status"}""", code, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val enrollment = AccountEnrollment(api)
        enrollment.requestVerification("user@example.test")
        enrollment.verify("synthetic-proof", "synthetic-password")
        enrollment.requestRecovery("user@example.test")
        enrollment.resetPassword("synthetic-proof", "synthetic-password", "synthetic-password")
        assertEquals(listOf("registrations", "verification", "recovery", "password"), calls)
        api.close()
    }

    @Test fun incorrectAcknowledgmentAndOversizedRequestFailClosed() = runTest {
        var calls = 0
        val api = AccountApi(ORIGIN, MockEngine { calls++; respond("""{"status":"password_updated"}""", HttpStatusCode.Accepted) })
        val enrollment = AccountEnrollment(api)
        assertEquals("invalid_response", assertFailsWith<SessionFailure> { enrollment.requestRecovery("a@example.test") }.code)
        assertEquals("invalid_request", assertFailsWith<SessionFailure> { enrollment.requestVerification("x".repeat(5000)) }.code)
        assertEquals(1, calls)
        api.close()
    }

    @Test fun enrollmentNeverSendsCredentialsToInsecureOrigin() = runTest {
        var calls = 0
        val api = AccountApi("http://remote.example.test", MockEngine { calls++; respond("{}") })
        assertEquals("insecure_origin", assertFailsWith<SessionFailure> {
            AccountEnrollment(api).verify("proof", "password")
        }.code)
        assertEquals(0, calls)
        api.close()
    }
}
