package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private var exitSeq = 0

class ExitInspectionTest {

    @Test
    fun exitMirrorsEntryRoomsWithLinks() = runTest {
        exitSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-exit-m-$exitSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val signer = platformSigner(dir)
        val sealer = LocalSealer(db, originals, signer)

        val tenancy = repo.createTenancy(repo.createProperty("Rua").id).id
        val entry = repo.createInspection(tenancy).id
        val sala = repo.addRoom(entry, "Sala")
        repo.setRoomNotApplicable(sala.id, setOf(ShotCategory.CEILING))
        repo.addRoom(entry, "Cozinha")
        val bytes = pdfTestPhoto()
        val stored = originals.storeOriginal("e1", bytes)
        repo.recordEvidence(entry, sala.id, "e1.bin", "image/jpeg", stored.size, stored.sha256, evidenceId = "e1")

        // Guards: no entry seal, no exit; double exit refused.
        assertFailsWith<LocalValidationException> { repo.startExitInspection(tenancy) }
        sealer.seal(entry)
        val exit = repo.startExitInspection(tenancy)
        assertEquals("exit", exit.status.let { db.inspectionDao().get(exit.id)!!.kind })
        assertFailsWith<LocalValidationException> { repo.startExitInspection(tenancy) }

        val mirrored = repo.roomsFor(exit.id)
        assertEquals(listOf("Sala", "Cozinha"), mirrored.map { it.label })
        assertEquals(listOf(0, 1), mirrored.map { it.sort })
        assertEquals(sala.id, mirrored.first { it.label == "Sala" }.sourceRoomId)
        assertEquals("CEILING", mirrored.first { it.label == "Sala" }.naCategories)
        assertTrue(mirrored.all { it.id != sala.id })
        assertTrue(mirrored.none { it.roomKey == sala.roomKey })

        // Exit seals independently; entry snapshot untouched.
        val exitEvidences = mirrored.first().let { room ->
            val data = pdfTestPhoto()
            val st = originals.storeOriginal("ex1", data)
            repo.recordEvidence(
                exit.id, room.id, "ex1.bin", "image/jpeg",
                st.size, st.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = "ex1",
            )
            repo.evidencesFor(exit.id)
        }
        assertEquals(listOf("ex1"), exitEvidences.map { it.id })
        sealer.seal(exit.id)
        assertEquals(STATUS_SEALED, db.inspectionDao().get(exit.id)!!.status)
        assertEquals(STATUS_SEALED, db.inspectionDao().get(entry)!!.status)
        db.close()
    }

    @Test
    fun exitRequiresExistingTenancyEntry() = runTest {
        exitSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-exit-g-$exitSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        assertFailsWith<LocalValidationException> { repo.startExitInspection("ghost") }
        val tenancy = repo.createTenancy(repo.createProperty("Rua").id).id
        assertFailsWith<LocalValidationException> { repo.startExitInspection(tenancy) }
        assertNull(repo.exitInspectionFor(tenancy))
        db.close()
    }
}
