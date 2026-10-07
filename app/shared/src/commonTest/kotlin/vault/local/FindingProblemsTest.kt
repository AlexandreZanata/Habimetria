package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var problemSeq = 0

class FindingProblemsTest {

    private fun freshProblems(): FindingProblemRepository {
        problemSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-problems-$problemSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        return FindingProblemRepository(localDatabaseBuilder("$dir/vault.db").build())
    }

    @Test
    fun seedsBuiltinsActiveWithPtLabels() = runTest {
        val repo = freshProblems()
        val all = repo.listAll()
        assertEquals(7, all.size)
        assertTrue(all.all { it.active && it.builtin })
        assertEquals("Trinca", all.first { it.code == "CRACK" }.label)
        assertEquals(7, repo.listActive().size)
    }

    @Test
    fun deactivationKeepsAtLeastOneActive() = runTest {
        val repo = freshProblems()
        repo.setActive("STAIN", false)
        assertEquals(6, repo.listActive().size)
        // Deactivate everything but CRACK, then the last one refuses.
        repo.listActive().filter { it.code != "CRACK" }.forEach { repo.setActive(it.code, false) }
        assertEquals(listOf("CRACK"), repo.listActive().map { it.code })
        assertFailsWith<LocalValidationException> { repo.setActive("CRACK", false) }
        assertFailsWith<LocalValidationException> { repo.setActive("NOPE", false) }
    }

    @Test
    fun customProblemsAddAndDelete() = runTest {
        val repo = freshProblems()
        val custom = repo.addCustom("  Infiltração no teto  ")
        assertEquals("Infiltração no teto", custom.label)
        assertTrue(!custom.builtin && custom.active)
        assertFailsWith<LocalValidationException> { repo.addCustom("   ") }
        assertFailsWith<LocalValidationException> { repo.deleteCustom("STAIN") }
        repo.deleteCustom(custom.code)
        assertEquals(null, repo.resolveCode(custom.code))
        assertFailsWith<LocalValidationException> { repo.deleteCustom(custom.code) }
    }

    @Test
    fun codeBasedFindingsRespectAvailability() = runTest {
        problemSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-problems-$problemSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val problems = FindingProblemRepository(db)
        val prop = repo.createProperty("Rua A, 1")
        val inspection = repo.createInspection(repo.createTenancy(prop.id).id).id
        val room = repo.addRoom(inspection, "Sala")
        val evidence = repo.recordEvidence(
            inspection, room.id, "a.bin", "image/jpeg", 10, "ab".repeat(32),
            evidenceId = "ev-prob-1",
        )
        val custom = problems.addCustom("Cupim")

        val stored = repo.addFindingWithCode(room.id, evidence.id, custom.code, null)
        assertEquals(custom.code, stored.preset)

        problems.setActive(custom.code, false)
        assertFailsWith<LocalValidationException> {
            repo.addFindingWithCode(room.id, evidence.id, custom.code, null)
        }
        assertFailsWith<LocalValidationException> {
            repo.addFindingWithCode(room.id, evidence.id, "NOPE", null)
        }
        db.close()
    }
}
