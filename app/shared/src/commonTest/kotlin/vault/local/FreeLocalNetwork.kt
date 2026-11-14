package vault.local

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import vault.api.VaultApi
import vault.sync.DbMirrorStore
import vault.sync.MirrorResult
import vault.sync.ServerMirror
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Expiration is modeled at the optional hosted boundary, not as an invented
// mobile subscription ledger. The real account/store lifecycle is later work.
internal enum class FreeLocalAccess { NEVER_REGISTERED, EXPIRED_HOSTED_ACCESS, OFFLINE }

internal class FreeLocalNetwork(private val access: FreeLocalAccess) {
    private var requests = 0
    private val engine = if (access == FreeLocalAccess.NEVER_REGISTERED) null else MockEngine {
        requests++
        if (access == FreeLocalAccess.OFFLINE) error("network unavailable")
        respond("hosted access expired", HttpStatusCode.PaymentRequired)
    }
    private val api = engine?.let { VaultApi("https://fixture.invalid", it) }

    suspend fun rememberHostedLink(session: LocalSession, propertyId: String) {
        if (access == FreeLocalAccess.EXPIRED_HOSTED_ACCESS) {
            DbMirrorStore(session.db).record(propertyId, "previously-linked-property")
        }
    }

    suspend fun assertNoTraffic(session: LocalSession, graph: FreeLocalGraph, exitId: String = graph.inspectionId) {
        assertEquals(SyncMode.LOCAL_ONLY, session.syncMode)
        assertFalse(session.uploadsAllowed)
        api?.let {
            val mirror = ServerMirror(session.db, it, session.syncMode, DbMirrorStore(session.db))
            assertEquals(MirrorResult.SkippedLocalOnly, mirror.mirrorOccurrence(graph.occurrenceId, graph.propertyId))
            assertEquals(MirrorResult.SkippedLocalOnly, mirror.mirrorExit(exitId, graph.tenancyId))
            assertEquals(MirrorResult.SkippedLocalOnly,
                mirror.mirrorComparison(graph.rooms[0].id, ShotCategory.OVERVIEW.name, graph.tenancyId) {
                    error("local work must not resolve server rooms")
                })
        }
        assertEquals(0, requests, "local operations must never consult the hosted adapter")
        val link = DbMirrorStore(session.db).remoteIdFor(graph.propertyId)
        assertEquals(if (access == FreeLocalAccess.EXPIRED_HOSTED_ACCESS) "previously-linked-property" else null, link)
    }

    fun close() { engine?.close() }
}
