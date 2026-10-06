package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DisplayRotationSealTest {
    @Test
    fun draftRotationIsFrozenAndSealedIsImmutable() = runTest {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-rotation-seal").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
        val signer = platformSigner(dir)
        try {
            val repo = session.repository
            val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua Rotacao").id).id).id
            val room = repo.addRoom(inspection, "Sala")
            val bytes = pdfTestPhoto()
            val stored = session.originals.storeOriginal("evRot", bytes)
            repo.recordEvidence(inspection, room.id, "evRot.bin", "image/jpeg",
                stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = "evRot")

            assertFailsWith<IllegalArgumentException> { repo.setDisplayRotation("evRot", 45) }
            session.correctEvidenceOrientation("evRot", 90)
            assertEquals(90, db.evidenceDao().get("evRot")!!.displayRotation)

            val manifest = (LocalSealer(db, session.originals, signer).seal(inspection) as SealOutcome.Sealed).manifest
            assertTrue(manifest.canonicalJson.contains("\"displayRotation\":90"))
            val snapshot = manifestSnapshot(manifest)
            assertEquals(90, snapshot.evidences.single().displayRotation)

            // Sealed rotation cannot change; originals stay intact.
            assertFailsWith<SealedModificationException> { repo.setDisplayRotation("evRot", 0) }
            assertFailsWith<SealedModificationException> { session.correctEvidenceOrientation("evRot", 0) }
            assertEquals(bytes.size, fs.read(session.originals.originalPath("evRot").toPath()) { readByteArray() }.size)
        } finally {
            session.close()
        }
    }

    @Test
    fun legacyManifestWithoutRotationDefaultsToZero() = runTest {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-rotation-legacy").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
        val signer = platformSigner(dir)
        try {
            val repo = session.repository
            val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua Legada").id).id).id
            val room = repo.addRoom(inspection, "Sala")
            val bytes = pdfTestPhoto()
            val stored = session.originals.storeOriginal("evLeg", bytes)
            repo.recordEvidence(inspection, room.id, "evLeg.bin", "image/jpeg",
                stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = "evLeg")
            val manifest = (LocalSealer(db, session.originals, signer).seal(inspection) as SealOutcome.Sealed).manifest
            // Missing optional field defaults to 0 for forward compatibility.
            val legacyJson = manifest.canonicalJson.replace("\"displayRotation\":0,", "")
            assertTrue(!legacyJson.contains("displayRotation"))
            val legacy = manifest.copy(canonicalJson = legacyJson)
            val snapshot = manifestSnapshot(legacy.copy(canonicalSha256 = manifestSha256(legacyJson)))
            assertEquals(0, snapshot.evidences.single().displayRotation)
            assertIs<SealOutcome.Sealed>(SealOutcome.Sealed(manifest))
        } finally {
            session.close()
        }
    }
}
