package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalManifest
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private var overviewSeq = 0

private fun freshOverviews(): Pair<VaultDatabase, LocalOverviewRepository> {
    overviewSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-overview-$overviewSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return db to LocalOverviewRepository(db)
}

class LocalOverviewsTest {

    @Test
    fun inspectionsReportsAndPendingResolveLabelsInBulk() = runTest {
        val (db, overviews) = freshOverviews()
        val repo = LocalVaultRepository(db)
        val nicknamed = repo.createPropertyStructured("Rua A", "1", null, "Centro", "Casa da praia")
        val plain = repo.createPropertyStructured("Rua B", "2", null, "Norte")
        val draft = repo.createInspection(repo.createTenancy(nicknamed.id).id).id
        val sealedReady = repo.createInspection(repo.createTenancy(plain.id).id).id
        val sealedPending = repo.createInspection(repo.createTenancy(plain.id).id).id
        for (id in listOf(sealedReady, sealedPending)) {
            db.inspectionDao().update(db.inspectionDao().get(id)!!.copy(status = STATUS_SEALED))
        }
        val manifest = LocalManifest(
            id = "man1", inspectionId = sealedReady, formatVersion = 1,
            canonicalSha256 = "ab".repeat(32), canonicalJson = "{}",
            sealedAtDevice = "2026-01-01T00:00:00", signerKeyId = "k1",
            algorithm = "ed25519", hardwareBacked = false, signature = "sig",
        )
        db.manifestDao().insert(manifest)
        db.reportDao().insert(
            LocalReport(
                id = "rep1", inspectionId = sealedReady, manifestId = manifest.id,
                manifestSha256 = manifest.canonicalSha256, status = REPORT_READY,
                pdfSha256 = "cd".repeat(32), pdfFileName = "rep1.pdf",
                receiptSignature = "rsig", signerKeyId = "k1", algorithm = "ed25519",
                hardwareBacked = false, createdAtDevice = "2026-01-01T00:00:00",
                completedAtDevice = "2026-01-01T00:00:01",
            ),
        )

        val all = overviews.inspectionOverviews()
        assertEquals(3, all.size)
        assertEquals("Casa da praia", all.first { it.inspection.id == draft }.propertyLabel)
        assertEquals(
            "Rua B, 2 — Norte",
            all.first { it.inspection.id == sealedReady }.propertyLabel,
        )
        assertEquals(1, overviews.inspectionOverviews(status = STATUS_DRAFT).size)
        assertEquals(2, overviews.inspectionOverviews(status = STATUS_SEALED).size)
        assertEquals(1, overviews.inspectionOverviews(limit = 1).size)

        val reports = overviews.reportOverviews()
        assertEquals(1, reports.size)
        assertEquals(sealedReady, reports.single().inspectionId)
        assertEquals("Rua B, 2 — Norte", reports.single().propertyLabel)

        val pending = overviews.pendingPdfs().map { it.inspectionId }
        assertTrue(sealedPending in pending)
        assertTrue(sealedReady !in pending)
        assertTrue(draft !in pending)
        db.close()
    }

    @Test
    fun emptyDatabaseReturnsEmptyOverviews() = runTest {
        val (db, overviews) = freshOverviews()
        assertTrue(overviews.inspectionOverviews().isEmpty())
        assertTrue(overviews.reportOverviews().isEmpty())
        assertTrue(overviews.pendingPdfs().isEmpty())
        db.close()
    }
}
