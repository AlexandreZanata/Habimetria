package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private var localDbSeq = 0

private fun tempDbPath(): String {
    localDbSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-local-$localDbSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return "$dir/vault.db"
}

private suspend fun seed(db: VaultDatabase) {
    db.propertyDao().insert(LocalProperty("p1", "Rua A, 123", "2026-09-30T10:00:00"))
    db.tenancyDao().insert(LocalTenancy("t1", "p1", "entry", "2026-09-30T10:01:00"))
    db.inspectionDao().insert(LocalInspection("i1", "t1", "entry", "DRAFT", "2026-09-30T10:02:00", null))
    db.roomDao().insert(LocalRoom("r1", "i1", "suite_principal", "Suíte principal", 0, ""))
    db.evidenceDao().insert(
        LocalEvidence(
            id = "e1", inspectionId = "i1", roomId = "r1", fileName = "ev-e1.bin",
            mime = "image/jpeg", size = 12, sha256 = "ab".repeat(32), sequence = 1,
            capturedAtDevice = "2026-09-30T10:03:00", origin = "capture",
            shotCategory = "overview", latitude = null, longitude = null,
            accuracyM = null, locationStatus = "unavailable",
        ),
    )
    db.findingDao().insert(LocalFinding("f1", "r1", "e1", "CRACK", "nota çãõ üñí"))
}

class LocalDatabaseTest {

    @Test
    fun seedCloseReopenPreservesInspectionAndEvidence() = runTest {
        val path = tempDbPath()
        val first = localDatabaseBuilder(path).build()
        seed(first)
        first.close()

        // Force-stop simulation: new process reopens the same file.
        val second = localDatabaseBuilder(path).build()
        assertEquals("DRAFT", assertNotNull(second.inspectionDao().get("i1")).status)
        assertEquals("Suíte principal", assertNotNull(second.roomDao().get("r1")).label)
        assertEquals(listOf("e1"), second.evidenceDao().listForInspection("i1").map { it.id })
        assertEquals(listOf("f1"), second.findingDao().listForRoom("r1").map { it.id })
        assertEquals(listOf("i1"), second.inspectionDao().listDrafts().map { it.id })
        assertEquals(1, second.evidenceDao().maxSequence("i1"))
        second.close()
    }
}
