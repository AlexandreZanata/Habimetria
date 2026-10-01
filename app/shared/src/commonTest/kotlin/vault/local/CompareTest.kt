package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private var compareSeq = 0

class CompareTest {

    @Test
    fun pairsMirrorRoomsWithLatestWins() = runTest {
        compareSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-cmp-$compareSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val compare = CompareRepository(db)

        val tenancy = repo.createTenancy(repo.createProperty("Rua").id).id
        val entry = repo.createInspection(tenancy).id
        val entryRoom = repo.addRoom(entry, "Sala")
        // Two overviews: latest must win the pair (e1 seals in WALLS).
        for ((id, payload, category) in listOf(
            Triple("ea1", "pa", ShotCategory.OVERVIEW),
            Triple("ea2", "pb", ShotCategory.OVERVIEW),
            Triple("e1", "x", ShotCategory.WALLS),
        )) {
            val stored = originals.storeOriginal(id, payload.encodeToByteArray())
            repo.recordEvidence(
                entry, entryRoom.id, "$id.bin", "image/jpeg",
                stored.size, stored.sha256, shotCategory = category, evidenceId = id,
            )
        }
        // Sealed entry then exit.
        val signer = platformSigner(dir)
        LocalSealer(db, originals, signer).seal(entry)
        val exit = repo.startExitInspection(tenancy)
        val exitRoom = repo.roomsFor(exit.id).single()
        repo.recordEvidence(exit.id, exitRoom.id, "x1.bin", "image/jpeg", 5, "cc", shotCategory = ShotCategory.WALLS, evidenceId = "ex1")

        val pairs = compare.pairsForExit(exit.id)
        assertEquals(5, pairs.size)
        assertTrue(pairs.all { it.roomLabel == "Sala" })
        val overview = pairs.first { it.category == ShotCategory.OVERVIEW }
        assertEquals("ea2", overview.entryEvidence?.id)
        assertNull(overview.exitEvidence)
        assertNull(overview.tag)
        val walls = pairs.first { it.category == ShotCategory.WALLS }
        assertEquals("e1", walls.entryEvidence?.id)
        assertEquals("ex1", walls.exitEvidence?.id)

        compare.setTag(tenancy, entryRoom.id, exitRoom.id, ShotCategory.OVERVIEW, CompareTag.CHANGED, "ea2", null)
        val retagged = compare.pairsForExit(exit.id).first { it.category == ShotCategory.OVERVIEW }
        assertEquals(CompareTag.CHANGED, retagged.tag)
        // Re-tag overwrites the single row per position.
        compare.setTag(tenancy, entryRoom.id, exitRoom.id, ShotCategory.OVERVIEW, CompareTag.WEAR, "ea2", null)
        assertEquals(1, compare.tagsForTenancy(tenancy).size)
        assertEquals(CompareTag.WEAR, compare.pairsForExit(exit.id).first { it.category == ShotCategory.OVERVIEW }.tag)
        db.close()
    }

    @Test
    fun tagSerialsMatchContract() {
        assertEquals("no_change", CompareTag.NO_CHANGE.serial)
        assertEquals("changed", CompareTag.CHANGED.serial)
        assertEquals("wear", CompareTag.WEAR.serial)
        assertEquals(CompareTag.WEAR, CompareTag.parse("wear"))
        assertNull(CompareTag.parse("liability"))
    }
}
