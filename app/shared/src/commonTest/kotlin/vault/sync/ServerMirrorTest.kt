package vault.sync

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.api.VaultApi
import vault.local.CompareTag
import vault.local.LocalOccurrenceRepository
import vault.local.LocalSealer
import vault.local.LocalVaultRepository
import vault.local.OriginalStore
import vault.local.ShotCategory
import vault.local.SyncMode
import vault.local.db.localDatabaseBuilder
import vault.local.platformSigner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private var mirrorSeq = 0

private fun tempDir(prefix: String): String {
    mirrorSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("$prefix-$mirrorSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir
}

private fun apiWith(body: String, counter: IntArray): VaultApi {
    val engine = MockEngine {
        counter[0]++
        respond(body, HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return VaultApi("http://test", engine)
}

class ServerMirrorTest {

    @Test
    fun localOnlyNeverTouchesNetwork() = runTest {
        val dir = tempDir("vault-mirror-off")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val vault = LocalVaultRepository(db)
        val property = vault.createProperty("Rua X, 1").id
        val occurrence = LocalOccurrenceRepository(db)
            .createOccurrence(property, "Leak", null).id
        val calls = intArrayOf(0)
        val mirror = ServerMirror(db, apiWith("{}", calls), SyncMode.LOCAL_ONLY)
        assertEquals(MirrorResult.SkippedLocalOnly, mirror.mirrorOccurrence(occurrence, "server-prop"))
        assertEquals(MirrorResult.SkippedLocalOnly, mirror.mirrorExit("ins-x", "server-ten"))
        assertEquals(
            MirrorResult.SkippedLocalOnly,
            mirror.mirrorComparison("room-x", "WALLS", "server-ten") { null },
        )
        assertEquals(0, calls[0])
        db.close()
    }

    @Test
    fun occurrenceMirrorsOnceThenShortCircuits() = runTest {
        val dir = tempDir("vault-mirror-occ")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val vault = LocalVaultRepository(db)
        val property = vault.createProperty("Rua Occ, 9").id
        val occurrence = LocalOccurrenceRepository(db)
            .createOccurrence(property, "Leak", null).id
        val calls = intArrayOf(0)
        val api = apiWith(
            """{"id":"o9","property_id":"sp1","title":"Leak","body":null,"created_at":"2026-09-25T10:00:00Z"}""",
            calls,
        )
        val store = InMemoryMirrorStore()
        val mirror = ServerMirror(db, api, SyncMode.WITH_SERVER_SYNC, store)
        assertEquals(MirrorResult.Mirrored("o9"), mirror.mirrorOccurrence(occurrence, "sp1"))
        assertEquals("o9", mirror.remoteIdFor(occurrence))
        // Second instance over the same store reuses the link offline.
        val silent = ServerMirror(
            db,
            VaultApi("http://test", MockEngine { error("must not call network") }),
            SyncMode.WITH_SERVER_SYNC,
            store,
        )
        assertEquals(MirrorResult.AlreadyMirrored("o9"), silent.mirrorOccurrence(occurrence, "sp1"))
        assertEquals(1, calls[0])
        db.close()
    }

    @Test
    fun occurrenceMissingLocallyFailsWithoutNetwork() = runTest {
        val dir = tempDir("vault-mirror-miss")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val calls = intArrayOf(0)
        val mirror = ServerMirror(db, apiWith("{}", calls), SyncMode.WITH_SERVER_SYNC)
        val result = mirror.mirrorOccurrence("ghost", "sp1")
        assertTrue(result is MirrorResult.Failed)
        assertEquals(0, calls[0])
        db.close()
    }

    @Test
    fun exitMirrorGuardsKindAndRecords() = runTest {
        val dir = tempDir("vault-mirror-exit")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val vault = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val tenancy = vault.createTenancy(vault.createProperty("Rua").id).id
        val entry = vault.createInspection(tenancy).id
        val room = vault.addRoom(entry, "Sala")
        val stored = originals.storeOriginal("e1", "b".encodeToByteArray())
        vault.recordEvidence(entry, room.id, "e1.bin", "image/jpeg", stored.size, stored.sha256, evidenceId = "e1")
        LocalSealer(db, originals, platformSigner(dir)).seal(entry)
        val exit = vault.startExitInspection(tenancy)
        val calls = intArrayOf(0)
        val api = apiWith(
            """{"id":"i9","tenancy_id":"st1","kind":"exit","status":"DRAFT","created_at":"2026-09-25T10:00:00Z"}""",
            calls,
        )
        val mirror = ServerMirror(db, api, SyncMode.WITH_SERVER_SYNC)
        assertTrue(mirror.mirrorExit(entry, "st1") is MirrorResult.Failed)
        assertEquals(MirrorResult.Mirrored("i9"), mirror.mirrorExit(exit.id, "st1"))
        assertEquals(1, calls[0])
        db.close()
    }

    @Test
    fun comparisonNeedsServerRoomsThenUpserts() = runTest {
        val dir = tempDir("vault-mirror-cmp")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val vault = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val tenancy = vault.createTenancy(vault.createProperty("Rua").id).id
        val entry = vault.createInspection(tenancy).id
        val entryRoom = vault.addRoom(entry, "Sala")
        val stored = originals.storeOriginal("ea", "pa".encodeToByteArray())
        vault.recordEvidence(
            entry, entryRoom.id, "ea.bin", "image/jpeg",
            stored.size, stored.sha256, shotCategory = ShotCategory.WALLS, evidenceId = "ea",
        )
        LocalSealer(db, originals, platformSigner(dir)).seal(entry)
        val exit = vault.startExitInspection(tenancy)
        val exitRoom = vault.roomsFor(exit.id).single()
        ServerMirror(db, apiWith("{}", intArrayOf(0)), SyncMode.WITH_SERVER_SYNC)
            .comparisonRepository().setTag(
                tenancy, entryRoom.id, exitRoom.id, ShotCategory.WALLS,
                CompareTag.CHANGED, "ea", null,
            )
        // Unresolvable server rooms fail before any network call.
        val noCalls = intArrayOf(0)
        val guarded = ServerMirror(db, apiWith("{}", noCalls), SyncMode.WITH_SERVER_SYNC)
        assertTrue(
            guarded.mirrorComparison(exitRoom.id, "WALLS", "st1") { null } is MirrorResult.Failed,
        )
        assertEquals(0, noCalls[0])
        // Resolved rooms upsert the tag; evidence links stay null.
        val upserts = intArrayOf(0)
        val api = apiWith(
            """{"id":"c9","tenancy_id":"st1","entry_room_id":"srv-r1","exit_room_id":"srv-r2","shot_category":"WALLS","entry_evidence_id":null,"exit_evidence_id":null,"tag":"changed","created_at":"2026-09-25T10:00:00Z"}""",
            upserts,
        )
        val mirror = ServerMirror(db, api, SyncMode.WITH_SERVER_SYNC)
        val result = mirror.mirrorComparison(exitRoom.id, "WALLS", "st1") { local ->
            if (local == entryRoom.id) "srv-r1" else "srv-r2"
        }
        assertEquals(MirrorResult.Mirrored("c9"), result)
        assertEquals(1, upserts[0])
        db.close()
    }

    @Test
    fun networkErrorSurfacesAsFailedForRetry() = runTest {
        val dir = tempDir("vault-mirror-err")
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val vault = LocalVaultRepository(db)
        val property = vault.createProperty("Rua").id
        val occurrence = LocalOccurrenceRepository(db)
            .createOccurrence(property, "Leak", null).id
        val api = VaultApi("http://test", MockEngine { error("offline") })
        val mirror = ServerMirror(db, api, SyncMode.WITH_SERVER_SYNC)
        val result = mirror.mirrorOccurrence(occurrence, "sp1")
        assertTrue(result is MirrorResult.Failed)
        assertEquals(null, mirror.remoteIdFor(occurrence))
        db.close()
    }
}
