package vault.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// VaultApi talks to the Go backend. Engine is platform-provided
// (CIO on Android/JVM, Darwin on iOS) for a light footprint.
class VaultApi private constructor(baseUrl: String, private val client: HttpClient) {
    private val base = baseUrl.trimEnd('/')

    constructor(baseUrl: String, engineFactory: HttpClientEngineFactory<*>) :
        this(baseUrl, HttpClient(engineFactory) { installJson() })

    constructor(baseUrl: String, engine: HttpClientEngine) :
        this(baseUrl, HttpClient(engine) { installJson() })

    suspend fun createProperty(address: String): Property =
        postJson("api/v1/properties", """{"address":"$address"}""")

    suspend fun createTenancy(propertyId: String): Tenancy =
        postJson("api/v1/properties/$propertyId/tenancies", "")

    suspend fun createInspection(tenancyId: String): Inspection =
        postJson("api/v1/tenancies/$tenancyId/inspections", "{}")

    suspend fun listRooms(inspectionId: String): List<Room> =
        getJson("api/v1/inspections/$inspectionId/rooms")

    suspend fun seal(inspectionId: String): SealResult =
        postJson("api/v1/inspections/$inspectionId/seal", "{}")

    private suspend inline fun <reified T> postJson(path: String, body: String): T {
        val text = client.post("$base/$path") {
            contentType(ContentType.Application.Json)
            if (body.isNotEmpty()) setBody(body)
        }.bodyAsText()
        return Json.decodeFromString(text)
    }

    private suspend inline fun <reified T> getJson(path: String): T {
        val text = client.get("$base/$path").bodyAsText()
        return Json.decodeFromString(text)
    }
}

fun HttpClientConfig<*>.installJson() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
