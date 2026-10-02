package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var inspectorSeq = 0

class AiInspectorTest {

    private fun freshDb(): Pair<String, vault.local.db.VaultDatabase> {
        inspectorSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-ai-$inspectorSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        return dir to localDatabaseBuilder("$dir/vault.db").build()
    }

    @Test
    fun gapsAndHintsAreIdempotent() = runTest {
        val (_, db) = freshDb()
        val repo = LocalVaultRepository(db)
        val inspector = AiInspectorRepository(db)

        val tenancy = repo.createTenancy(repo.createProperty("Rua AI").id).id
        val inspection = repo.createInspection(tenancy).id
        val room = repo.addRoom(inspection, "Sala")
        repo.recordEvidence(
            inspection, room.id, "w1.bin", "image/jpeg", 5, "aa",
            shotCategory = ShotCategory.WALLS, evidenceId = "ew1",
        )

        val first = inspector.runInspection(inspection)
        // 4 missing categories (all but WALLS) + stain/crack for the wall photo.
        assertEquals(6, first.size)
        assertEquals(4, first.count { it.kind == SuggestionKind.COVERAGE_GAP.serial })
        assertEquals(2, first.count {
            it.kind == SuggestionKind.POSSIBLE_STAIN.serial ||
                it.kind == SuggestionKind.POSSIBLE_CRACK.serial
        })

        val second = inspector.runInspection(inspection)
        assertEquals(6, second.size)

        val gaps = inspector.coverageGaps(inspection)
        assertEquals(1, gaps.size)
        assertEquals(4, gaps.single().missing.size)
        assertTrue(ShotCategory.WALLS !in gaps.single().missing)
        db.close()
    }

    @Test
    fun confirmCreatesFindingAndIgnoreDismisses() = runTest {
        val (_, db) = freshDb()
        val repo = LocalVaultRepository(db)
        val inspector = AiInspectorRepository(db)

        val tenancy = repo.createTenancy(repo.createProperty("Rua Confirm").id).id
        val inspection = repo.createInspection(tenancy).id
        val room = repo.addRoom(inspection, "Quarto")
        repo.recordEvidence(
            inspection, room.id, "f1.bin", "image/jpeg", 5, "bb",
            shotCategory = ShotCategory.FLOOR, evidenceId = "ef1",
        )
        inspector.runInspection(inspection)

        val stain = inspector.pendingForInspection(inspection)
            .first { it.kind == SuggestionKind.POSSIBLE_STAIN.serial }
        val finding = inspector.confirm(stain.id, "mancha perto da porta")
        assertEquals("STAIN", finding.preset)
        assertEquals("ef1", finding.evidenceId)

        val crack = inspector.pendingForInspection(inspection)
            .first { it.kind == SuggestionKind.POSSIBLE_CRACK.serial }
        inspector.ignore(crack.id)
        assertEquals(
            SuggestionStatus.IGNORED.serial,
            db.aiSuggestionDao().get(crack.id)!!.status,
        )

        // Decided hints refuse a second decision; gaps never become findings.
        assertFailsWith<LocalValidationException> { inspector.confirm(stain.id) }
        val gap = inspector.pendingForInspection(inspection)
            .first { it.kind == SuggestionKind.COVERAGE_GAP.serial }
        assertFailsWith<LocalValidationException> { inspector.confirm(gap.id) }
        db.close()
    }

    @Test
    fun evidenceWithFindingGetsNoHintAndSealedRefuses() = runTest {
        val (dir, db) = freshDb()
        val repo = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val inspector = AiInspectorRepository(db)

        val tenancy = repo.createTenancy(repo.createProperty("Rua Seal").id).id
        val inspection = repo.createInspection(tenancy).id
        val room = repo.addRoom(inspection, "Cozinha")
        val stored = originals.storeOriginal("eseal", pdfTestPhoto())
        repo.recordEvidence(
            inspection, room.id, "c1.bin", "image/jpeg",
            stored.size, stored.sha256,
            shotCategory = ShotCategory.CEILING, evidenceId = "eseal",
        )
        repo.addFinding(room.id, "eseal", vault.findings.FindingPreset.CRACK, null)

        val hints = inspector.runInspection(inspection)
        assertTrue(hints.none { it.evidenceId == "eseal" })

        LocalSealer(db, originals, platformSigner(dir)).seal(inspection)
        assertFailsWith<SealedModificationException> { inspector.runInspection(inspection) }
        db.close()
    }

    @Test
    fun kindSerialsMatchContract() {
        assertEquals("coverage_gap", SuggestionKind.COVERAGE_GAP.serial)
        assertEquals("possible_stain", SuggestionKind.POSSIBLE_STAIN.serial)
        assertEquals("possible_crack", SuggestionKind.POSSIBLE_CRACK.serial)
    }
}
