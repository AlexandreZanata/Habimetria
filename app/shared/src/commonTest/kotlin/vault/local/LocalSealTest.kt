package vault.local

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

private var sealSeq = 0

private fun sealHarness(): Triple<VaultDatabase, LocalSession, InstallationSigner> {
    sealSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-seal-$sealSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
    return Triple(db, session, platformSigner(dir))
}

private suspend fun seedCaptured(session: LocalSession, address: String = "Rua Seal, 7"): String {
    val repo = session.repository
    val inspection = repo.createInspection(repo.createTenancy(repo.createProperty(address).id).id).id
    val room = repo.addRoom(inspection, "Suíte principal")
    val bytes = "sealed-photo-bytes".encodeToByteArray()
    val evidenceId = newLocalId("ev")
    val stored = session.originals.storeOriginal(evidenceId, bytes)
    repo.recordEvidence(
        inspection, room.id, "$evidenceId.bin", "image/jpeg",
        stored.size, stored.sha256, shotCategory = ShotCategory.OVERVIEW, evidenceId = evidenceId,
    )
    val evidence = repo.evidencesForRoom(room.id).single()
    repo.addFinding(room.id, evidence.id, FindingPreset.CRACK, "trinca")
    return inspection
}

class LocalSealTest {

    @Test
    fun sealFreezesSignedSnapshot() = runTest {
        val (db, session, signer) = sealHarness()
        val inspectionId = seedCaptured(session)
        val sealer = LocalSealer(db, session.originals, signer)

        val eligibility = sealer.assess(inspectionId)
        assertTrue(eligibility.canSeal)
        assertEquals(1, eligibility.rooms)
        assertEquals(1, eligibility.evidences)
        assertEquals(1, eligibility.findings)

        val outcome = sealer.seal(inspectionId)
        assertIs<SealOutcome.Sealed>(outcome)
        val manifest = outcome.manifest
        assertEquals(3, manifest.formatVersion)
        assertEquals(signer.keyId, manifest.signerKeyId)
        assertEquals(SEAL_ALGORITHM, manifest.algorithm)
        assertTrue(manifest.canonicalJson.contains("Suíte principal"))
        assertTrue(sealer.verifyManifest(manifest, signer.publicKeyUncompressed()))
        assertEquals(STATUS_SEALED, db.inspectionDao().get(inspectionId)!!.status)

        // Retry returns the identical snapshot, never a second row.
        val retry = sealer.seal(inspectionId)
        assertIs<SealOutcome.AlreadySealed>(retry)
        assertEquals(manifest.id, retry.manifest.id)
        assertEquals(manifest.canonicalSha256, retry.manifest.canonicalSha256)
        db.close()
    }

    @Test
    fun concurrentSealsCollapseToOneRow() = runTest {
        val (db, session, signer) = sealHarness()
        val inspectionId = seedCaptured(session)
        val sealer = LocalSealer(db, session.originals, signer)

        val outcomes = awaitAll(async { sealer.seal(inspectionId) }, async { sealer.seal(inspectionId) })
        val hashes = outcomes.map {
            when (it) {
                is SealOutcome.Sealed -> it.manifest.canonicalSha256
                is SealOutcome.AlreadySealed -> it.manifest.canonicalSha256
            }
        }
        assertEquals(1, hashes.toSet().size)
        db.close()
    }

    @Test
    fun emptyAndUnreadableBlockSeal() = runTest {
        val (db, session, signer) = sealHarness()
        val repo = session.repository
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua").id).id).id
        val sealer = LocalSealer(db, session.originals, signer)

        val empty = assertFailsWith<SealBlockedException> { sealer.seal(inspection) }
        assertTrue(empty.reasons.contains(SEAL_EMPTY))

        val room = repo.addRoom(inspection, "Sala")
        val bytes = "x".encodeToByteArray()
        val stored = session.originals.storeOriginal("evX", bytes)
        repo.recordEvidence(
            inspection, room.id, "evX.bin", "image/jpeg",
            stored.size, stored.sha256, evidenceId = "evX",
        )
        FileSystem.SYSTEM.delete(session.originals.originalPath("evX").toPath())
        val missing = assertFailsWith<SealBlockedException> { sealer.seal(inspection) }
        assertTrue(missing.reasons.any { it.startsWith(SEAL_UNREADABLE) })
        // Failed seal keeps DRAFT terminal-state clean: still editable.
        assertEquals(STATUS_DRAFT, db.inspectionDao().get(inspection)!!.status)
        db.close()
    }

    @Test
    fun alterationInvalidatesVerification() = runTest {
        val (db, session, signer) = sealHarness()
        val sealer = LocalSealer(db, session.originals, signer)
        val manifest = (sealer.seal(seedCaptured(session)) as SealOutcome.Sealed).manifest

        val tampered = manifest.copy(canonicalJson = manifest.canonicalJson.replace("Suíte", "Suite"))
        assertTrue(sealer.verifyManifest(manifest, signer.publicKeyUncompressed()))
        // Room label, note, GPS or hash change: same verdict path.
        assertTrue(!sealer.verifyManifest(tampered, signer.publicKeyUncompressed()))
        assertTrue(!sealer.verifyManifest(manifest.copy(signature = "00".repeat(64)), signer.publicKeyUncompressed()))
        db.close()
    }
}
