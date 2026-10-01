package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.Comparison
import vault.local.db.InspectionBlockReason
import vault.local.db.InspectionDeleteOutcome
import vault.local.db.InspectionRemovalPlan
import vault.local.db.LocalManifest
import vault.local.db.LocalReport
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private var inspectionRemovalSeq = 0

private fun removalSession(): LocalSession {
    inspectionRemovalSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-inspection-removal-$inspectionRemovalSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    return LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY, fs)
}

private suspend fun seedDraft(session: LocalSession): Triple<String, String, String> {
    val repo = session.repository
    val prop = repo.createProperty("Rua A, 1")
    val inspection = repo.createInspection(repo.createTenancy(prop.id).id).id
    val room = repo.addRoom(inspection, "Sala")
    val stored = session.originals.storeOriginal("ins-rem-ev", "photo-bytes".encodeToByteArray())
    val evidence = repo.recordEvidence(
        inspection, room.id, "ins-rem-ev.bin", "image/jpeg",
        stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = "ins-rem-ev",
    )
    repo.addFinding(room.id, evidence.id, FindingPreset.CRACK, "trinca")
    return Triple(prop.id, inspection, room.id)
}

class InspectionRemovalTest {

    @Test
    fun draftDeleteRemovesRowsAndFilesKeepsProperty() = runTest {
        val session = removalSession()
        val (propertyId, inspectionId, roomId) = seedDraft(session)
        val originalPath = session.originals.originalPath("ins-rem-ev")

        assertIs<InspectionDeleteOutcome.Deleted>(session.inspectionRemoval.delete(inspectionId))
        assertEquals(null, session.repository.getInspection(inspectionId))
        assertTrue(session.repository.roomsFor(inspectionId).isEmpty())
        assertTrue(session.db.findingDao().listForRoom(roomId).isEmpty())
        assertFalse(FileSystem.SYSTEM.exists(originalPath.toPath()))
        // Tenancy and property survive: inspection deletion alone never
        // unblocks property deletion.
        assertTrue(session.repository.getProperty(propertyId) != null)
        assertIs<InspectionDeleteOutcome.Missing>(session.inspectionRemoval.delete(inspectionId))
        assertIs<InspectionDeleteOutcome.Missing>(session.inspectionRemoval.delete("ins_missing"))
        session.close()
    }

    @Test
    fun sealedDeleteRemovesManifestReportAndPdf() = runTest {
        val session = removalSession()
        val (_, inspectionId, _) = seedDraft(session)
        val inspection = session.repository.getInspection(inspectionId)!!
        session.db.inspectionDao().update(inspection.copy(status = STATUS_SEALED))
        val manifest = LocalManifest(
            id = "man-del", inspectionId = inspectionId, formatVersion = 1,
            canonicalSha256 = "ab".repeat(32), canonicalJson = "{}",
            sealedAtDevice = "2026-01-01T00:00:00", signerKeyId = "k1",
            algorithm = "ed25519", hardwareBacked = false, signature = "sig",
        )
        session.db.manifestDao().insert(manifest)
        session.db.reportDao().insert(
            LocalReport(
                id = "rep-del", inspectionId = inspectionId, manifestId = manifest.id,
                manifestSha256 = manifest.canonicalSha256, status = REPORT_READY,
                pdfSha256 = "cd".repeat(32), pdfFileName = "rep-del.pdf",
                receiptSignature = "rsig", signerKeyId = "k1", algorithm = "ed25519",
                hardwareBacked = false, createdAtDevice = "2026-01-01T00:00:00",
                completedAtDevice = "2026-01-01T00:00:01",
            ),
        )
        val fs = FileSystem.SYSTEM
        fs.createDirectories("${session.filesDir}/vault/reports".toPath())
        fs.write("${session.filesDir}/vault/reports/rep-del.pdf".toPath()) { write(byteArrayOf(1)) }
        fs.write("${session.filesDir}/vault/reports/rep-del-receipt.json".toPath()) { writeUtf8("{}") }

        val plan = session.inspectionRemoval.eligibility(inspectionId)
        assertIs<InspectionRemovalPlan.Ready>(plan)
        assertTrue(plan.sealed)
        assertIs<InspectionDeleteOutcome.Deleted>(session.inspectionRemoval.delete(inspectionId))
        assertEquals(null, session.db.manifestDao().getByInspection(inspectionId))
        assertEquals(null, session.db.reportDao().get("rep-del"))
        assertFalse(fs.exists("${session.filesDir}/vault/reports/rep-del.pdf".toPath()))
        assertFalse(fs.exists("${session.filesDir}/vault/reports/rep-del-receipt.json".toPath()))
        session.close()
    }

    @Test
    fun comparedInspectionIsBlockedAndPreserved() = runTest {
        val session = removalSession()
        val (_, inspectionId, roomId) = seedDraft(session)
        val tenancyId = session.repository.getInspection(inspectionId)!!.tenancyId
        session.db.comparisonDao().upsert(
            Comparison(
                id = "cmp-del", tenancyId = tenancyId, entryRoomId = roomId,
                exitRoomId = "room-other", shotCategory = ShotCategory.WALLS.name,
                entryEvidenceId = null, exitEvidenceId = null, tag = "no_change",
                createdAtDevice = "2026-01-01T00:00:00",
            ),
        )

        val attempt = session.inspectionRemoval.delete(inspectionId)
        assertIs<InspectionDeleteOutcome.Blocked>(attempt)
        assertEquals(InspectionBlockReason.HAS_COMPARISONS, attempt.reason)
        assertTrue(session.repository.getInspection(inspectionId) != null)
        assertTrue(session.repository.roomsFor(inspectionId).isNotEmpty())
        session.close()
    }

    @Test
    fun staleComparisonBlocksInsideTransaction() = runTest {
        val session = removalSession()
        val (_, inspectionId, roomId) = seedDraft(session)
        val plan = session.inspectionRemoval.eligibility(inspectionId)
        assertIs<InspectionRemovalPlan.Ready>(plan)
        val tenancyId = session.repository.getInspection(inspectionId)!!.tenancyId
        session.db.comparisonDao().upsert(
            Comparison(
                id = "cmp-stale", tenancyId = tenancyId, entryRoomId = roomId,
                exitRoomId = "room-other", shotCategory = ShotCategory.FLOOR.name,
                entryEvidenceId = null, exitEvidenceId = null, tag = "changed",
                createdAtDevice = "2026-01-01T00:00:00",
            ),
        )
        val attempt = session.inspectionRemoval.delete(inspectionId)
        assertIs<InspectionDeleteOutcome.Blocked>(attempt)
        assertTrue(session.repository.getInspection(inspectionId) != null)
        session.close()
    }
}
