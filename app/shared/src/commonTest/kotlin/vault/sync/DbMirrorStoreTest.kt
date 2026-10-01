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
import vault.local.LocalOccurrenceRepository
import vault.local.LocalVaultRepository
import vault.local.SyncMode
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

private var dbMirrorSeq = 0

class DbMirrorStoreTest {

    @Test
    fun linksSurviveCloseAndReopen() = runTest {
        dbMirrorSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-dbmirror-$dbMirrorSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val path = "$dir/vault.db"
        val first = localDatabaseBuilder(path).build()
        DbMirrorStore(first).record("occ_1", "remote-1")
        assertEquals("remote-1", DbMirrorStore(first).remoteIdFor("occ_1"))
        first.close()

        val second = localDatabaseBuilder(path).build()
        assertEquals("remote-1", DbMirrorStore(second).remoteIdFor("occ_1"))
        // First write wins: no duplicate server rows on retry.
        DbMirrorStore(second).record("occ_1", "remote-2")
        assertEquals("remote-1", DbMirrorStore(second).remoteIdFor("occ_1"))
        assertEquals(null, DbMirrorStore(second).remoteIdFor("ghost"))
        second.close()
    }

    @Test
    fun serverMirrorReusesDbLinksAfterRestartWithoutNetwork() = runTest {
        dbMirrorSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-dbmirror-restart-$dbMirrorSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val path = "$dir/vault.db"
        val first = localDatabaseBuilder(path).build()
        val vault = LocalVaultRepository(first)
        val property = vault.createProperty("Rua X, 1").id
        val occurrence = LocalOccurrenceRepository(first)
            .createOccurrence(property, "Leak", null).id
        val engine = MockEngine {
            respond(
                """{"id":"o9","property_id":"sp1","title":"Leak","body":null,"created_at":"2026-09-25T10:00:00Z"}""",
                HttpStatusCode.Created,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val mirrored = ServerMirror(first, VaultApi("http://test", engine), SyncMode.WITH_SERVER_SYNC, DbMirrorStore(first))
        assertEquals(MirrorResult.Mirrored("o9"), mirrored.mirrorOccurrence(occurrence, "sp1"))
        first.close()

        // Relaunch: the recorded link short-circuits before any network.
        val second = localDatabaseBuilder(path).build()
        val offline = ServerMirror(
            second,
            VaultApi("http://test", MockEngine { error("must not call network") }),
            SyncMode.WITH_SERVER_SYNC,
            DbMirrorStore(second),
        )
        assertEquals(MirrorResult.AlreadyMirrored("o9"), offline.mirrorOccurrence(occurrence, "sp1"))
        second.close()
    }
}
