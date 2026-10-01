package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.MirrorLink
import vault.local.db.PropertyArchiveOutcome
import vault.local.db.PropertyBlockReason
import vault.local.db.PropertyDeleteOutcome
import vault.local.db.PropertyRestoreOutcome
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private var removalSeq = 0

private fun removalSession(): LocalSession {
    removalSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-removal-$removalSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
}

private fun reopenSession(dir: String, fs: FileSystem = FileSystem.SYSTEM): LocalSession {
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
}

class PropertyRemovalTest {

    @Test
    fun emptyDeleteSucceedsAndRepeatsAsMissing() = runTest {
        val session = removalSession()
        val prop = session.repository.createPropertyStructured("Rua A", "1", null, "Centro")
        assertFalse(session.db.propertyDao().get(prop.id)!!.archived)
        assertIs<PropertyDeleteOutcome.Deleted>(session.propertyRemoval.deleteEmpty(prop.id))
        assertEquals(null, session.db.propertyDao().get(prop.id))
        assertIs<PropertyDeleteOutcome.Missing>(session.propertyRemoval.deleteEmpty(prop.id))
        assertIs<PropertyDeleteOutcome.Missing>(session.propertyRemoval.deleteEmpty("prop_missing"))
        session.close()
    }

    @Test
    fun populatedDeleteBlockedAndPreservesRows() = runTest {
        val session = removalSession()
        val withTenancy = session.repository.createPropertyStructured("Rua B", "2", null, "Norte")
        session.repository.createTenancy(withTenancy.id)
        val withOccurrence = session.repository.createPropertyStructured("Rua C", "3", null, "Sul")
        session.occurrences.createOccurrence(withOccurrence.id, "Infiltração", null)
        val withMirror = session.repository.createPropertyStructured("Rua D", "4", null, "Leste")
        session.db.mirrorLinkDao().insert(MirrorLink(withMirror.id, "remote-1", deviceNowIso()))

        val tenancyBlock = session.propertyRemoval.deleteEmpty(withTenancy.id)
        assertIs<PropertyDeleteOutcome.Blocked>(tenancyBlock)
        assertEquals(PropertyBlockReason.HAS_TENANCIES, tenancyBlock.reason)
        val occurrenceBlock = session.propertyRemoval.deleteEmpty(withOccurrence.id)
        assertIs<PropertyDeleteOutcome.Blocked>(occurrenceBlock)
        assertEquals(PropertyBlockReason.HAS_OCCURRENCES, occurrenceBlock.reason)
        val mirrorBlock = session.propertyRemoval.deleteEmpty(withMirror.id)
        assertIs<PropertyDeleteOutcome.Blocked>(mirrorBlock)
        assertEquals(PropertyBlockReason.HAS_MIRROR_LINK, mirrorBlock.reason)

        assertTrue(session.db.propertyDao().get(withTenancy.id) != null)
        assertTrue(session.db.propertyDao().get(withOccurrence.id) != null)
        assertTrue(session.db.propertyDao().get(withMirror.id) != null)
        session.close()
    }

    @Test
    fun staleEligibilityCannotAuthorizeAfterConcurrentTenancy() = runTest {
        val session = removalSession()
        val prop = session.repository.createPropertyStructured("Rua E", "5", null, "Oeste")
        val before = session.propertyRemoval.eligibility(prop.id)!!
        assertTrue(before.canDelete)
        session.repository.createTenancy(prop.id)
        val attempt = session.propertyRemoval.deleteEmpty(prop.id)
        assertIs<PropertyDeleteOutcome.Blocked>(attempt)
        assertEquals(PropertyBlockReason.HAS_TENANCIES, attempt.reason)
        assertTrue(session.db.propertyDao().get(prop.id) != null)
        session.close()
    }

    @Test
    fun archiveRestoreSurvivesReopenWithSealIntact() = runTest {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-removal-seal").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())
        var session = LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY, fs)
        val prop = session.repository.createPropertyStructured("Rua F", "6", null, "Centro")
        val inspection = session.repository.createInspection(session.repository.createTenancy(prop.id).id).id
        val room = session.repository.addRoom(inspection, "Sala")
        val evidenceId = newLocalId("ev")
        val stored = session.originals.storeOriginal(evidenceId, "photo-bytes".encodeToByteArray())
        session.repository.recordEvidence(
            inspection, room.id, "$evidenceId.bin", "image/jpeg",
            stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = evidenceId,
        )
        session.repository.addFinding(room.id, evidenceId, FindingPreset.CRACK, "trinca")
        val sealer = LocalSealer(session.db, session.originals, platformSigner(dir))
        val manifest = (sealer.seal(inspection) as SealOutcome.Sealed).manifest
        assertIs<PropertyArchiveOutcome.Archived>(session.propertyRemoval.archive(prop.id))
        assertIs<PropertyArchiveOutcome.AlreadyArchived>(session.propertyRemoval.archive(prop.id))
        assertIs<PropertyDeleteOutcome.Blocked>(session.propertyRemoval.deleteEmpty(prop.id))
        session.close()

        session = reopenSession(dir, fs)
        assertTrue(session.db.propertyDao().get(prop.id)!!.archived)
        assertEquals(STATUS_SEALED, session.db.inspectionDao().get(inspection)!!.status)
        assertEquals(manifest.canonicalSha256, session.db.manifestDao().getByInspection(inspection)!!.canonicalSha256)
        assertIs<PropertyRestoreOutcome.Restored>(session.propertyRemoval.restore(prop.id))
        assertIs<PropertyRestoreOutcome.AlreadyActive>(session.propertyRemoval.restore(prop.id))
        session.close()

        session = reopenSession(dir, fs)
        assertFalse(session.db.propertyDao().get(prop.id)!!.archived)
        assertEquals(manifest.canonicalSha256, session.db.manifestDao().getByInspection(inspection)!!.canonicalSha256)
        assertEquals(listOf(inspection), session.repository.inspectionsForProperty(prop.id).map { it.id })
        session.close()
    }

    @Test
    fun storageFailurePropagatesInsteadOfFalseSuccess() = runTest {
        val session = removalSession()
        val prop = session.repository.createPropertyStructured("Rua G", "7", null, "Centro")
        val db: VaultDatabase = session.db
        session.close()
        assertFailsWith<Exception> { db.propertyRemovalDao().deleteIfEmpty(prop.id) }
    }
}
