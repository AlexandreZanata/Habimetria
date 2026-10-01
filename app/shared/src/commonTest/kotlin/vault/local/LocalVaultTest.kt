package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var vaultSeq = 0

private fun freshVault(): Pair<VaultDatabase, LocalVaultRepository> {
    vaultSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-repo-$vaultSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return db to LocalVaultRepository(db)
}

private suspend fun seedInspection(repo: LocalVaultRepository): String {
    val prop = repo.createProperty("Rua das Flores, 42")
    val ten = repo.createTenancy(prop.id)
    return repo.createInspection(ten.id).id
}

class LocalVaultTest {

    @Test
    fun offlineJourneyCreatesGraphWithoutNetwork() = runTest {
        val (db, repo) = freshVault()
        val inspectionId = seedInspection(repo)
        val room = repo.addRoom(inspectionId, "Suíte principal çãõ")
        assertEquals("Suíte principal çãõ", room.label)
        assertTrue(room.roomKey.isNotBlank())

        val ev = repo.recordEvidence(
            inspectionId = inspectionId, roomId = room.id, fileName = "ev-1.bin",
            mime = "image/jpeg", size = 100, sha256 = "ff".repeat(32),
            shotCategory = ShotCategory.OVERVIEW,
        )
        assertEquals(1, ev.sequence)
        val ev2 = repo.recordEvidence(
            inspectionId = inspectionId, roomId = room.id, fileName = "ev-2.bin",
            mime = "image/jpeg", size = 100, sha256 = "aa".repeat(32),
            shotCategory = ShotCategory.OVERVIEW,
        )
        assertEquals(2, ev2.sequence)

        val finding = repo.addFinding(room.id, ev.id, FindingPreset.CRACK, " trinca ")
        assertEquals("CRACK", finding.preset)
        assertEquals(" trinca ", finding.note)
        db.close()
    }

    @Test
    fun repeatedCategoryNeverCompletesChecklist() = runTest {
        val (db, repo) = freshVault()
        val coverage = VaultCoverage(db)
        val inspectionId = seedInspection(repo)
        val room = repo.addRoom(inspectionId, "Garagem")
        repeat(5) { i ->
            repo.recordEvidence(
                inspectionId, room.id, "ev-$i.bin", "image/jpeg", 10, "bb".repeat(32),
                shotCategory = ShotCategory.OVERVIEW,
            )
        }
        assertEquals(20, coverage.roomCoverage(room.id).percent())
        assertEquals(
            listOf(ShotCategory.WALLS, ShotCategory.OPENINGS, ShotCategory.FLOOR, ShotCategory.CEILING),
            coverage.missingCategories(room.id),
        )
        repo.setRoomNotApplicable(room.id, setOf(ShotCategory.CEILING))
        assertEquals(25, coverage.roomCoverage(room.id).percent())
        db.close()
    }

    @Test
    fun roomValidationAndDeleteGuards() = runTest {
        val (db, repo) = freshVault()
        val inspectionId = seedInspection(repo)
        assertFailsWith<LocalValidationException> { repo.addRoom(inspectionId, "  ") }
        assertFailsWith<LocalValidationException> { repo.addRoom(inspectionId, "x".repeat(81)) }
        assertFailsWith<LocalValidationException> { repo.createProperty("ab") }

        val room = repo.addRoom(inspectionId, "Cozinha")
        repo.recordEvidence(inspectionId, room.id, "ev.bin", "image/jpeg", 10, "cc".repeat(32))
        assertFailsWith<LocalValidationException> { repo.deleteRoom(room.id) }
        // Even explicit confirmation refuses while evidence rows remain attached.
        assertFailsWith<LocalValidationException> { repo.deleteRoom(room.id, allowWithEvidence = true) }

        val empty = repo.addRoom(inspectionId, "Vazia")
        repo.deleteRoom(empty.id)
        assertEquals(listOf("Cozinha"), repo.roomsFor(inspectionId).map { it.label })
        db.close()
    }

    @Test
    fun reorderNormalizesSortOrder() = runTest {
        val (db, repo) = freshVault()
        val inspectionId = seedInspection(repo)
        val a = repo.addRoom(inspectionId, "A")
        val b = repo.addRoom(inspectionId, "B")
        val c = repo.addRoom(inspectionId, "C")

        val moved = repo.moveRoom(c.id, 0)
        assertEquals(listOf(c.id, a.id, b.id), moved.map { it.id })
        assertEquals(listOf(0, 1, 2), moved.map { it.sort })

        val clamped = repo.moveRoom(a.id, 99)
        assertEquals(listOf(c.id, b.id, a.id), clamped.map { it.id })
        assertEquals(listOf(0, 1, 2), clamped.map { it.sort })
        db.close()
    }

    @Test
    fun sealedInspectionFreezesEdits() = runTest {
        val (db, repo) = freshVault()
        val inspectionId = seedInspection(repo)
        val room = repo.addRoom(inspectionId, "Sala")
        db.inspectionDao().update(
            db.inspectionDao().get(inspectionId)!!.copy(status = STATUS_SEALED),
        )
        assertFailsWith<SealedModificationException> { repo.renameRoom(room.id, "Nova") }
        assertFailsWith<SealedModificationException> { repo.addRoom(inspectionId, "Outra") }
        assertFailsWith<SealedModificationException> { repo.moveRoom(room.id, 0) }
        assertFailsWith<SealedModificationException> {
            repo.recordEvidence(inspectionId, room.id, "x.bin", "image/jpeg", 1, "dd".repeat(32))
        }
        db.close()
    }

    @Test
    fun findingRejectsForeignEvidence() = runTest {
        val (db, repo) = freshVault()
        val a = seedInspection(repo)
        val b = seedInspection(repo)
        val roomA = repo.addRoom(a, "A")
        val roomB = repo.addRoom(b, "B")
        val ev = repo.recordEvidence(a, roomA.id, "ev.bin", "image/jpeg", 10, "ee".repeat(32))
        assertFailsWith<LocalValidationException> {
            repo.addFinding(roomB.id, ev.id, FindingPreset.STAIN, null)
        }
        db.close()
    }
}
