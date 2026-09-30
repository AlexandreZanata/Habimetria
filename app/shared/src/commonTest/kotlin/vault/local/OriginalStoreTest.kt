package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var originalSeq = 0

private fun tempFilesDir(): String {
    originalSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-orig-$originalSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir
}

class OriginalStoreTest {

    @Test
    fun storeThenReadKeepsExactBytes() {
        val dir = tempFilesDir()
        val store = OriginalStore(FileSystem.SYSTEM, dir)
        val bytes = "photo-bytes-çã".encodeToByteArray()
        val stored = store.storeOriginal("ev1", bytes)
        assertEquals(bytes.size.toLong(), stored.size)
        assertEquals(store.sha256Of(bytes), stored.sha256)
        assertTrue(store.readOriginal("ev1").contentEquals(bytes))
        // No .tmp residue after a clean write.
        assertFalse(FileSystem.SYSTEM.exists("$dir/vault/originals/ev1.bin.tmp".toPath()))
    }

    @Test
    fun refusesEmptyAndDuplicateOriginals() {
        val store = OriginalStore(FileSystem.SYSTEM, tempFilesDir())
        assertFailsWith<LocalStoreException> { store.storeOriginal("e", ByteArray(0)) }
        store.storeOriginal("e", "x".encodeToByteArray())
        assertFailsWith<LocalStoreException> { store.storeOriginal("e", "y".encodeToByteArray()) }
        assertFailsWith<LocalStoreException> { store.readOriginal("ghost") }
    }

    @Test
    fun reconcileReportsMissingAndOrphanTmp() = runTest {
        val dir = tempFilesDir()
        val store = OriginalStore(FileSystem.SYSTEM, dir)
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val prop = repo.createProperty("Rua X")
        val inspection = repo.createInspection(repo.createTenancy(prop.id).id)
        val room = repo.addRoom(inspection.id, "Quarto")
        val stored = store.storeOriginal("ev1", "bytes".encodeToByteArray())
        repo.recordEvidence(
            inspection.id, room.id, "ev1.bin", "image/jpeg",
            stored.size, stored.sha256, evidenceId = "ev1",
        )

        val clean = store.reconcile(db)
        assertEquals(1, clean.checked)
        assertFalse(clean.hasProblems())

        // Simulate disk loss plus an interrupted write.
        FileSystem.SYSTEM.delete(store.originalPath("ev1").toPath())
        FileSystem.SYSTEM.createDirectories("$dir/vault/originals".toPath())
        FileSystem.SYSTEM.write("$dir/vault/originals/ev2.bin.tmp".toPath()) { writeUtf8("half") }

        val dirty = store.reconcile(db)
        assertTrue(dirty.hasProblems())
        assertEquals(listOf("ev1"), dirty.missingFiles)
        assertEquals(listOf("ev2.bin.tmp"), dirty.orphanTmps)
        db.close()
    }
}
