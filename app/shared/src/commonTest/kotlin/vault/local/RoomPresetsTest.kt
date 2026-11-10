package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var presetSeq = 0

private fun freshPresetVault(): Pair<VaultDatabase, LocalVaultRepository> {
    presetSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-presets-$presetSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return db to LocalVaultRepository(db)
}

private suspend fun seedDraftInspection(repo: LocalVaultRepository): String {
    val prop = repo.createProperty("Rua dos Presets, 7")
    val ten = repo.createTenancy(prop.id)
    return repo.createInspection(ten.id).id
}

class RoomPresetsTest {

    @Test
    fun addRenameDeleteRoundtrip() = runTest {
        val (db, _) = freshPresetVault()
        val presets = RoomPresetRepository(db)
        assertTrue(presets.list().isEmpty())

        val sala = presets.add(" Sala ")
        val cozinha = presets.add("Cozinha")
        assertEquals(listOf("Sala", "Cozinha"), presets.list().map { it.label })

        presets.rename(sala.id, "Sala de estar")
        assertEquals(listOf("Sala de estar", "Cozinha"), presets.list().map { it.label })

        presets.delete(cozinha.id)
        assertEquals(listOf("Sala de estar"), presets.list().map { it.label })
        db.close()
    }

    @Test
    fun duplicateAndInvalidLabelsRejected() = runTest {
        val (db, _) = freshPresetVault()
        val presets = RoomPresetRepository(db)
        presets.add("Quarto")
        assertFailsWith<LocalValidationException> { presets.add("quarto") }
        assertFailsWith<LocalValidationException> { presets.add("   ") }
        assertFailsWith<LocalValidationException> { presets.add("x".repeat(MAX_ROOM_LABEL + 1)) }
        val solo = presets.add("Banheiro")
        assertFailsWith<LocalValidationException> { presets.rename(solo.id, "QUARTO") }
        db.close()
    }

    @Test
    fun applyCopiesIntoDraftSkippingExistingLabels() = runTest {
        val (db, repo) = freshPresetVault()
        val presets = RoomPresetRepository(db)
        presets.add("Sala")
        presets.add("Cozinha")
        presets.add("Quarto")

        val inspectionId = seedDraftInspection(repo)
        repo.addRoom(inspectionId, "sala")

        val after = repo.addRoomsFromPresets(inspectionId)
        assertEquals(listOf("sala", "Cozinha", "Quarto"), after.map { it.label })
        // Second apply changes nothing: no duplicates ever.
        val again = repo.addRoomsFromPresets(inspectionId)
        assertEquals(after.map { it.id }, again.map { it.id })
        db.close()
    }

    @Test
    fun applyRefusesSealedInspectionAndPresetsStayEditable() = runTest {
        val (db, repo) = freshPresetVault()
        val presets = RoomPresetRepository(db)
        presets.add("Sala")
        val inspectionId = seedDraftInspection(repo)
        val room = repo.addRoom(inspectionId, "Sala")

        // Flip status to SEALED directly: the seal pipeline itself is
        // covered by LocalSealTest; here only the DRAFT guard matters.
        val draft = db.inspectionDao().get(inspectionId)!!
        db.inspectionDao().update(draft.copy(status = STATUS_SEALED))
        assertFailsWith<SealedModificationException> {
            repo.addRoomsFromPresets(inspectionId)
        }
        db.inspectionDao().update(draft)

        assertTrue(room.label == "Sala")
        // Preset list itself keeps working while inspections are sealed.
        presets.rename(presets.list().single().id, "Sala íntima")
        assertEquals("Sala íntima", presets.list().single().label)
        db.close()
    }
}
