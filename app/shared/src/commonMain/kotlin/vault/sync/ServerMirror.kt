package vault.sync

import vault.api.VaultApi
import vault.local.CompareRepository
import vault.local.LocalOccurrenceRepository
import vault.local.SyncMode
import vault.local.db.VaultDatabase

// MirrorResult keeps local durability and server sync visibly separate.
// A mirrored row is never relabeled CONFIRMED locally; callers map
// these outcomes to SyncIndicator explicitly (see LocalSession).
sealed interface MirrorResult {
    data class Mirrored(val remoteId: String) : MirrorResult
    data class AlreadyMirrored(val remoteId: String) : MirrorResult
    data object SkippedLocalOnly : MirrorResult
    data class Failed(val reason: String) : MirrorResult
}

// MirrorStore remembers localId -> remoteId so retries stay idempotent.
// The in-memory default suits tests; DbMirrorStore survives relaunch.
interface MirrorStore {
    suspend fun remoteIdFor(localId: String): String?
    suspend fun record(localId: String, remoteId: String)
}

class InMemoryMirrorStore : MirrorStore {
    private val links = mutableMapOf<String, String>()

    override suspend fun remoteIdFor(localId: String): String? = links[localId]

    override suspend fun record(localId: String, remoteId: String) {
        links[localId] = remoteId
    }
}

// ServerMirror pushes user-confirmed local rows to the optional API.
// Local rows stay the source of truth: failures return Failed (never
// throw), LOCAL_ONLY never touches the network, and photo bytes are
// out of scope (UploadPump owns them). Evidence references in
// comparisons mirror as null: server evidences live in a separate
// id space and tag history stays on device.
class ServerMirror(
    private val db: VaultDatabase,
    private val api: VaultApi,
    private val mode: SyncMode,
    private val store: MirrorStore = InMemoryMirrorStore(),
) {
    private val occurrences = LocalOccurrenceRepository(db)
    private val compare = CompareRepository(db)

    suspend fun remoteIdFor(localId: String): String? = store.remoteIdFor(localId)

    suspend fun mirrorOccurrence(localId: String, serverPropertyId: String): MirrorResult {
        if (mode != SyncMode.WITH_SERVER_SYNC) return MirrorResult.SkippedLocalOnly
        store.remoteIdFor(localId)?.let { return MirrorResult.AlreadyMirrored(it) }
        val local = db.occurrenceDao().get(localId)
            ?: return MirrorResult.Failed("occurrence not found")
        return try {
            val remote = api.createOccurrence(serverPropertyId, local.title, local.body)
            store.record(localId, remote.id)
            MirrorResult.Mirrored(remote.id)
        } catch (e: Exception) {
            MirrorResult.Failed(e.message?.take(200) ?: "occurrence mirror failed")
        }
    }

    suspend fun mirrorExit(localExitId: String, serverTenancyId: String): MirrorResult {
        if (mode != SyncMode.WITH_SERVER_SYNC) return MirrorResult.SkippedLocalOnly
        store.remoteIdFor(localExitId)?.let { return MirrorResult.AlreadyMirrored(it) }
        val local = db.inspectionDao().get(localExitId)
            ?: return MirrorResult.Failed("inspection not found")
        if (local.kind != "exit") return MirrorResult.Failed("not an exit inspection")
        return try {
            val remote = api.createExitInspection(serverTenancyId)
            store.record(localExitId, remote.id)
            MirrorResult.Mirrored(remote.id)
        } catch (e: Exception) {
            MirrorResult.Failed(e.message?.take(200) ?: "exit mirror failed")
        }
    }

    // Comparison tags mirror with server room ids supplied by the caller
    // (local and server ids live in different spaces). Unresolvable rooms
    // fail explicitly instead of sending a wrong link.
    suspend fun mirrorComparison(
        exitRoomId: String,
        shotCategory: String,
        serverTenancyId: String,
        resolveServerRoom: (localRoomId: String) -> String?,
    ): MirrorResult {
        if (mode != SyncMode.WITH_SERVER_SYNC) return MirrorResult.SkippedLocalOnly
        val local = db.comparisonDao().get(exitRoomId, shotCategory)
            ?: return MirrorResult.Failed("comparison not found")
        store.remoteIdFor(local.id)?.let { return MirrorResult.AlreadyMirrored(it) }
        val serverEntry = resolveServerRoom(local.entryRoomId)
            ?: return MirrorResult.Failed("no server room for entry")
        val serverExit = resolveServerRoom(local.exitRoomId)
            ?: return MirrorResult.Failed("no server room for exit")
        return try {
            val remote = api.upsertComparison(
                tenancyId = serverTenancyId,
                entryRoomId = serverEntry,
                exitRoomId = serverExit,
                shotCategory = local.shotCategory,
                entryEvidenceId = null,
                exitEvidenceId = null,
                tag = local.tag,
            )
            store.record(local.id, remote.id)
            MirrorResult.Mirrored(remote.id)
        } catch (e: Exception) {
            MirrorResult.Failed(e.message?.take(200) ?: "comparison mirror failed")
        }
    }

    // Exposed for workers that list before mirroring; UI layers keep
    // using LocalSession repositories directly.
    fun occurrenceRepository(): LocalOccurrenceRepository = occurrences

    fun comparisonRepository(): CompareRepository = compare
}
