package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private var occurrenceSeq = 0

class OccurrenceTest {

    @Test
    fun createAttachAndList() = runTest {
        occurrenceSeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-occ-$occurrenceSeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val originals = OriginalStore(FileSystem.SYSTEM, dir)
        val vault = LocalVaultRepository(db)
        val occurrences = LocalOccurrenceRepository(db)
        val property = vault.createProperty("Rua Occ, 9").id

        assertFailsWith<LocalValidationException> { occurrences.createOccurrence(property, "  ", null) }
        assertFailsWith<LocalValidationException> { occurrences.createOccurrence("ghost", "x", null) }

        val occurrence = occurrences.createOccurrence(property, "Vazamento na cozinha", "sob a pia")
        assertTrue(occurrence.title.startsWith("Vazamento"))

        val bytes = "occurrence-photo".encodeToByteArray()
        val stored = originals.storeOriginal("att1", bytes)
        val attachment = occurrences.attachPhoto(
            occurrence.id, "att1.bin", "image/jpeg",
            stored.size, stored.sha256, note = "foto 1", attachmentId = "att1",
        )
        assertEquals("foto 1", attachment.note)
        assertEquals(listOf("att1"), occurrences.attachmentsFor(occurrence.id).map { it.id })
        assertEquals(listOf(occurrence.id), occurrences.listForProperty(property).map { it.id })
        assertFailsWith<LocalValidationException> {
            occurrences.attachPhoto("ghost", "x.bin", "image/jpeg", 1, "aa")
        }
        db.close()
    }

    @Test
    fun sealedInspectionStaysUntouched() = runTest {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-occ-seal").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, FileSystem.SYSTEM)
        val inspection = session.repository.createInspection(
            session.repository.createTenancy(session.repository.createProperty("Rua").id).id,
        ).id
        val room = session.repository.addRoom(inspection, "Sala")
        val stored = session.originals.storeOriginal("e1", "b".encodeToByteArray())
        session.repository.recordEvidence(
            inspection, room.id, "e1.bin", "image/jpeg",
            stored.size, stored.sha256, evidenceId = "e1",
        )
        val signer = platformSigner(dir)
        LocalSealer(db, session.originals, signer).seal(inspection)
        val before = db.manifestDao().getByInspection(inspection)!!.canonicalSha256

        // Post-seal story goes to occurrences, keyed by property.
        val occurrences = LocalOccurrenceRepository(db)
        val property = occurrences.propertyForInspection(inspection)
        occurrences.createOccurrence(property, "Nova mancha", null)

        assertEquals(1, occurrences.listForProperty(property).size)
        assertEquals(STATUS_SEALED, db.inspectionDao().get(inspection)!!.status)
        assertEquals(before, db.manifestDao().getByInspection(inspection)!!.canonicalSha256)
        session.close()
    }
}
