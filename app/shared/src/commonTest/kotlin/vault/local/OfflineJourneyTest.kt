package vault.local

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var journeySeq = 0

private class JourneyRenderer : PlanRenderer {
    val photos = mutableListOf<String>()
    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray {
        pages.forEachIndexed { i, page ->
            page.items.filterIsInstance<PageItem.Photo>().forEach {
                photos.add(it.entry.evidenceId)
                loadPhoto(it.entry.evidenceId)
            }
            onProgress(i + 1, pages.size)
        }
        return "journey-pdf".encodeToByteArray()
    }
}

// The complete local journey with no network anywhere in the path:
// create → name rooms → capture with categories → findings → seal →
// render → export → verify. Uses only the local session; VaultApi and
// upload workers never enter the picture (LOCAL_ONLY gate).
class OfflineJourneyTest {

    @Test
    fun fullJourneyOfflineEndToEnd() = runTest {
        journeySeq++
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-journey-$journeySeq").toString()
        fs.deleteRecursively(dir.toPath())
        fs.createDirectories(dir.toPath())

        // API stopped, networking blocked: only the local session exists.
        val session = openLocalSession(dir, SyncMode.LOCAL_ONLY)
        assertFalse(session.uploadsAllowed)
        val startup = session.startup()
        assertEquals(0, startup.reconcile.checked)

        val repo = session.repository
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua L8, 1").id).id).id
        session.navigation.save(NavigationProgress(inspection))
        val suite = repo.addRoom(inspection, "Suíte principal")
        val garage = repo.addRoom(inspection, "Garagem")
        repo.moveRoom(garage.id, 0)

        val signer = platformSigner(dir)
        val categories = listOf(ShotCategory.OVERVIEW, ShotCategory.WALLS, ShotCategory.CEILING)
        for ((index, category) in categories.withIndex()) {
            val bytes = pdfTestPhoto()
            val id = newLocalId("ev")
            val stored = session.originals.storeOriginal(id, bytes)
            // Derivatives need real JPEG codecs; covered separately with
            // a fake scaler. The pipeline falls back to originals.
            repo.recordEvidence(
                inspection, suite.id, "$id.bin", "image/jpeg",
                stored.size, stored.sha256, shotCategory = category, evidenceId = id,
                location = LocationClaim(-23.5, -46.5, 8f, 1_000, "gps", "fine", LocationStatus.PRECISE),
                exif = readExifGps(bytes),
            )
        }
        val firstEvidence = repo.evidencesForRoom(suite.id).first()
        repo.addFinding(suite.id, firstEvidence.id, FindingPreset.CRACK, "trinca na parede")
        assertEquals(60, session.coverage.roomCoverage(suite.id).percent())

        val sealer = LocalSealer(session.db, session.originals, signer)
        val manifest = (sealer.seal(inspection) as SealOutcome.Sealed).manifest
        assertTrue(sealer.verifyManifest(manifest, signer.publicKeyUncompressed()))

        val generator = ReportGenerator(session.db, dir, session.originals, session.derivatives, signer)
        val renderer = JourneyRenderer()
        val report = generator.generate(inspection, renderer)
        assertEquals(REPORT_READY, report.status)
        assertEquals(3, renderer.photos.toSet().size)

        val exporter = PackageExporter(session.db, dir, session.originals, signer)
        val packageDir = exporter.export(report.id)
        val verdict = PackageVerifier(signer).verify(packageDir, pinnedKeys = setOf(signer.keyId))
        assertTrue(verdict.accepted())
        assertEquals(IssuerTrust.PINNED, verdict.issuerTrust)

        // Force-stop analogue: reopen everything from disk.
        session.close()
        val resumed = openLocalSession(dir, SyncMode.LOCAL_ONLY)
        assertEquals(NavigationProgress(inspection), resumed.navigation.load())
        assertEquals(STATUS_SEALED, resumed.repository.getInspection(inspection)!!.status)
        assertEquals(3, resumed.repository.evidencesFor(inspection).size)
        val reverify = ReportGenerator(resumed.db, dir, resumed.originals, resumed.derivatives, signer)
            .verify(report.id)
        assertTrue(reverify.allOk())
        resumed.close()
    }

    @Test
    fun concurrentCapturesNeverShareSequence() = runTest {
        journeySeq++
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-conc-$journeySeq").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val db = localDatabaseBuilder("$dir/vault.db").build()
        val repo = LocalVaultRepository(db)
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua").id).id).id
        val room = repo.addRoom(inspection, "Sala")

        val ids = awaitAll(async {
            repo.recordEvidence(inspection, room.id, "a.bin", "image/jpeg", 5, "aa", evidenceId = "ca")
        }, async {
            repo.recordEvidence(inspection, room.id, "b.bin", "image/jpeg", 5, "bb", evidenceId = "cb")
        }).map { it.sequence }.toSet()
        assertEquals(setOf(1, 2), ids)
        db.close()
    }

    @Test
    fun receiptBytesAreDeterministic() {
        val first = receiptBytes("rep1", "man1", "m".repeat(64), "p".repeat(64), "k".repeat(64))
        val second = receiptBytes("rep1", "man1", "m".repeat(64), "p".repeat(64), "k".repeat(64))
        assertTrue(first.contentEquals(second))
    }
}
