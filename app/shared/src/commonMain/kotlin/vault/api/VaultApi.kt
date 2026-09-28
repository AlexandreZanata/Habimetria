package vault.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import vault.queue.EvidenceRemote

// VaultApi talks to the Go backend. Engine is platform-provided
// (CIO on Android/JVM, Darwin on iOS) for a light footprint.
class VaultApi private constructor(baseUrl: String, private val client: HttpClient) : EvidenceRemote {
    private var base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    // retarget switches server at runtime (dev: emulator vs LAN device).
    fun retarget(baseUrl: String) {
        base = baseUrl.trimEnd('/')
    }

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

    suspend fun ensureTemplate(inspectionId: String): List<Room> {
        val text = client.post("$base/api/v1/inspections/$inspectionId/rooms:ensure-template").bodyAsText()
        return json.decodeFromString(text)
    }

    suspend fun coverage(inspectionId: String): Coverage =
        getJson("api/v1/inspections/$inspectionId/coverage")

    suspend fun createFinding(roomId: String, evidenceId: String, preset: String, note: String?): Finding =
        postJson(
            "api/v1/rooms/$roomId/findings",
            json.encodeToString(FindingBody(evidenceId, preset, note?.takeIf { it.isNotBlank() })),
        )

    suspend fun listFindings(roomId: String): List<Finding> =
        getJson("api/v1/rooms/$roomId/findings")

    suspend fun reportDetail(reportId: String): ReportDetail =
        getJson("api/v1/reports/$reportId")

    suspend fun attachPdf(reportId: String, pdf: ByteArray): Report {
        val text = client.post("$base/api/v1/reports/$reportId/pdf") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", pdf, HeadersBuilder().apply {
                            append(HttpHeaders.ContentType, "application/pdf")
                            append(HttpHeaders.ContentDisposition, "filename=\"report.pdf\"")
                        }.build())
                    },
                ),
            )
        }.bodyAsText()
        return json.decodeFromString(text)
    }

    suspend fun verify(code: String): VerifyResult =
        getJson("v/$code")

    suspend fun evidences(inspectionId: String): List<EvidenceLine> =
        getJson("api/v1/inspections/$inspectionId/evidences")

    suspend fun evidenceDownloadUrl(inspectionId: String, evidenceId: String): String {
        val text = client.get("$base/api/v1/inspections/$inspectionId/evidences/download-url?evidence_id=$evidenceId").bodyAsText()
        return json.decodeFromString<Map<String, String>>(text)["url"] ?: error("no url")
    }

    suspend fun downloadBytes(url: String): ByteArray =
        client.get(url).body()

    override suspend fun uploadPhoto(
        inspectionId: String,
        roomId: String?,
        bytes: ByteArray,
        mime: String,
    ): Evidence {
        val text = client.post("$base/api/v1/inspections/$inspectionId/evidences") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", bytes, HeadersBuilder().apply {
                            append(HttpHeaders.ContentType, mime)
                            append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                        }.build())
                        append("type", "photo")
                        if (roomId != null) append("room_id", roomId)
                    },
                ),
            )
        }.bodyAsText()
        return json.decodeFromString(text)
    }

    private suspend inline fun <reified T> postJson(path: String, body: String): T {
        val text = client.post("$base/$path") {
            contentType(ContentType.Application.Json)
            if (body.isNotEmpty()) setBody(body)
        }.bodyAsText()
        return json.decodeFromString(text)
    }

    private suspend inline fun <reified T> getJson(path: String): T {
        val text = client.get("$base/$path").bodyAsText()
        return json.decodeFromString(text)
    }
}

fun HttpClientConfig<*>.installJson() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
