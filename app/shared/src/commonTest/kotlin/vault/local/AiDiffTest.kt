package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var diffSeq = 0

class AiDiffTest {

    private fun freshDb(): Pair<String, vault.local.db.VaultDatabase> {
        diffSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-dif-$diffSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        return dir to localDatabaseBuilder("$dir/vault.db").build()
    }

    private suspend fun sealedEntryWithExit(
        dir: String,
        db: vault.local.db.VaultDatabase,
        entryShots: List<Triple<String, String, ShotCategory>>,
        exitShots: List<Triple<String, String, ShotCategory>>,
    ): Triple<String, String, String> {
        val repo = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val tenancy = repo.createTenancy(repo.createProperty("Rua Diff").id).id
        val entry = repo.createInspection(tenancy).id
        val entryRoom = repo.addRoom(entry, "Sala")
        for ((id, payload, category) in entryShots) {
            val stored = originals.storeOriginal(id, pdfTestPhoto() + payload.encodeToByteArray())
            repo.recordEvidence(
                entry, entryRoom.id, "$id.bin", "image/jpeg",
                stored.size, stored.sha256, shotCategory = category, evidenceId = id,
            )
        }
        LocalSealer(db, originals, platformSigner(dir)).seal(entry)
        val exit = repo.startExitInspection(tenancy).id
        val exitRoom = repo.roomsFor(exit).single()
        for ((id, payload, category) in exitShots) {
            repo.recordEvidence(
                exit, exitRoom.id, "$id.bin", "image/jpeg",
                payload.length.toLong().coerceAtLeast(1), "sha-$id-$payload",
                shotCategory = category, evidenceId = id,
            )
        }
        return Triple(tenancy, entry, exit)
    }

    @Test
    fun missingExitPhotoRaisesPresenceChange() = runTest {
        val (dir, db) = freshDb()
        val (_, _, exit) = sealedEntryWithExit(
            dir, db,
            entryShots = listOf(Triple("en1", "entry-bytes", ShotCategory.WALLS)),
            exitShots = emptyList(),
        )
        val rows = VisualDiffRepository(db).runDiff(exit)
        // Every entry-covered position without an exit photo is listed.
        assertEquals(1, rows.size)
        assertTrue(DiffSignal.PRESENCE_CHANGE in parseDiffSignals(rows.single().signals))
        db.close()
    }

    @Test
    fun byteOnlyDifferenceRaisesNoRow() = runTest {
        val (dir, db) = freshDb()
        val (_, _, exit) = sealedEntryWithExit(
            dir, db,
            entryShots = listOf(Triple("en1", "entry-bytes", ShotCategory.WALLS)),
            exitShots = listOf(Triple("ex1", "exit-bytes", ShotCategory.WALLS)),
        )
        // Same position, same metadata, different bytes: expected for any
        // two captures, so the list stays empty.
        assertTrue(VisualDiffRepository(db).runDiff(exit).isEmpty())
        db.close()
    }

    @Test
    fun findingsTagAndLocationChangesSurface() = runTest {
        diffSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-dif-loc-$diffSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val tenancy = repo.createTenancy(repo.createProperty("Rua Diff 2").id).id
        val entry = repo.createInspection(tenancy).id
        val entryRoom = repo.addRoom(entry, "Sala")
        val stored = originals.storeOriginal("en2", pdfTestPhoto())
        repo.recordEvidence(
            entry, entryRoom.id, "en2.bin", "image/jpeg",
            stored.size, stored.sha256, shotCategory = ShotCategory.FLOOR, evidenceId = "en2",
        )
        repo.addFinding(entryRoom.id, "en2", vault.findings.FindingPreset.CRACK, null)
        LocalSealer(db, originals, platformSigner(dir)).seal(entry)
        val exit = repo.startExitInspection(tenancy).id
        val exitRoom = repo.roomsFor(exit).single()
        repo.recordEvidence(
            exit, exitRoom.id, "ex2.bin", "image/jpeg", 5, "sha-ex2",
            shotCategory = ShotCategory.FLOOR, evidenceId = "ex2",
            location = LocationClaim(null, null, null, null, null, null, LocationStatus.DENIED),
        )
        val diff = VisualDiffRepository(db)
        val rows = diff.runDiff(exit)
        assertEquals(1, rows.size)
        val signals = parseDiffSignals(rows.single().signals)
        assertTrue(DiffSignal.FINDINGS_CHANGED in signals)
        assertTrue(DiffSignal.LOCATION_CHANGED in signals)

        // User tag corroboration joins the same row on rerun; the pending
        // row is refreshed, never duplicated.
        diff.runDiff(exit)
        assertEquals(1, diff.listForExit(exit).size)
        CompareRepository(db).setTag(tenancy, entryRoom.id, exitRoom.id, ShotCategory.FLOOR, CompareTag.CHANGED, "en2", "ex2")
        val retagged = diff.runDiff(exit)
        assertEquals(1, retagged.size)
        assertTrue(DiffSignal.USER_TAGGED in parseDiffSignals(retagged.single().signals))
        db.close()
    }

    @Test
    fun reviewAndDismissDecideOnce() = runTest {
        val (dir, db) = freshDb()
        val (_, _, exit) = sealedEntryWithExit(
            dir, db,
            entryShots = listOf(Triple("en1", "entry-bytes", ShotCategory.WALLS)),
            exitShots = emptyList(),
        )
        val diff = VisualDiffRepository(db)
        val row = diff.runDiff(exit).single()
        assertEquals(DiffStatus.REVIEWED.serial, diff.markReviewed(row.id).status)
        assertTrue(diff.pendingForExit(exit).isEmpty())
        assertFailsWith<LocalValidationException> { diff.dismiss(row.id) }

        // Decided rows keep history: a rerun does not reopen them.
        assertEquals(DiffStatus.REVIEWED.serial, diff.runDiff(exit).single().status)
        db.close()
    }

    @Test
    fun exitWithoutEntryThrows() = runTest {
        val (_, db) = freshDb()
        val repo = LocalVaultRepository(db)
        val tenancy = repo.createTenancy(repo.createProperty("Rua Solo").id).id
        val orphanExit = repo.createInspection(tenancy, "exit").id
        assertFailsWith<LocalValidationException> { VisualDiffRepository(db).runDiff(orphanExit) }
        assertFailsWith<LocalValidationException> { VisualDiffRepository(db).runDiff("missing") }
        db.close()
    }

    @Test
    fun signalSerialsMatchContract() {
        assertEquals("presence_change", DiffSignal.PRESENCE_CHANGE.serial)
        assertEquals("findings_changed", DiffSignal.FINDINGS_CHANGED.serial)
        assertEquals("user_tagged", DiffSignal.USER_TAGGED.serial)
        assertEquals("location_changed", DiffSignal.LOCATION_CHANGED.serial)
        assertEquals("bytes_differ", DiffSignal.BYTES_DIFFER.serial)
    }
}
