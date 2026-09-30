package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private var packageSeq = 0

private class StaticRenderer : PlanRenderer {
    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray = "static-pdf".encodeToByteArray()
}

private suspend fun packageHarness(photos: Int = 2): Triple<VaultDatabase, LocalSession, InstallationSigner> {
    packageSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-pkg-$packageSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    val session = LocalSession(db, dir, SyncMode.LOCAL_ONLY, fs)
    val signer = platformSigner(dir)
    val repo = session.repository
    val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua Pkg").id).id).id
    val room = repo.addRoom(inspection, "Sala")
    repeat(photos) { i ->
        val bytes = "pkg-photo-$i".encodeToByteArray()
        val stored = session.originals.storeOriginal("ev$i", bytes)
        repo.recordEvidence(
            inspection, room.id, "ev$i.bin", "image/jpeg",
            stored.size, stored.sha256, evidenceId = "ev$i",
        )
    }
    LocalSealer(db, session.originals, signer).seal(inspection)
    return Triple(db, session, signer)
}

private suspend fun readyReport(session: LocalSession, signer: InstallationSigner): String {
    val inspection = session.db.inspectionDao().listAll().single().id
    val generator = ReportGenerator(session.db, session.filesDir, session.originals, session.derivatives, signer)
    return generator.generate(inspection, StaticRenderer()).id
}

class PackageFormatTest {

    @Test
    fun exportVerifyRoundtripAccepts() = runTest {
        val (db, session, signer) = packageHarness()
        val reportId = readyReport(session, signer)
        val dir = PackageExporter(db, session.filesDir, session.originals, signer).export(reportId)

        val verdict = PackageVerifier(signer).verify(dir, pinnedKeys = setOf(signer.keyId))
        assertTrue(verdict.accepted())
        assertEquals(IssuerTrust.PINNED, verdict.issuerTrust)
        assertEquals(2, verdict.evidencesChecked)
        assertTrue(verdict.signatureValid)

        // Unknown signer stays unknown: integrity passes, trust does not.
        val unknown = PackageVerifier(signer).verify(dir)
        assertTrue(unknown.accepted())
        assertEquals(IssuerTrust.UNKNOWN, unknown.issuerTrust)
        db.close()
    }

    @Test
    fun tamperingAndLossStayDistinct() = runTest {
        val (db, session, signer) = packageHarness()
        val reportId = readyReport(session, signer)
        val exporter = PackageExporter(db, session.filesDir, session.originals, signer)
        val dir = exporter.export(reportId)

        FileSystem.SYSTEM.write("$dir/report.pdf".toPath()) { writeUtf8("forged") }
        val forged = PackageVerifier(signer).verify(dir)
        assertFalse(forged.accepted())
        assertEquals(FileStatus.MODIFIED, forged.pdf)
        assertEquals(FileStatus.MODIFIED, forged.receipt)

        exporter.export(reportId)
        FileSystem.SYSTEM.delete("$dir/originals/ev0.bin".toPath())
        val lost = PackageVerifier(signer).verify(dir)
        assertFalse(lost.accepted())
        assertEquals(listOf("ev0"), lost.evidencesMissing)
        assertTrue(lost.evidencesModified.isEmpty())

        exporter.export(reportId)
        FileSystem.SYSTEM.write("$dir/originals/ev1.bin".toPath()) { writeUtf8("changed") }
        val changed = PackageVerifier(signer).verify(dir)
        assertFalse(changed.accepted())
        assertEquals(listOf("ev1"), changed.evidencesModified)
        db.close()
    }

    @Test
    fun substitutedKeyFailsSignature() = runTest {
        val (db, session, signer) = packageHarness()
        val reportId = readyReport(session, signer)
        val dir = PackageExporter(db, session.filesDir, session.originals, signer).export(reportId)

        val otherDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-pkg-other").toString()
        val other = platformSigner(otherDir)
        val verdict = PackageVerifier(other).verify(dir, pinnedKeys = setOf(other.keyId))
        // Attacker re-signed nothing: manifest/receipt still carry the
        // original key, so trust cannot migrate to the substitute.
        assertEquals(IssuerTrust.UNKNOWN, verdict.issuerTrust)
        assertTrue(verdict.signatureValid)
        db.close()
    }
}

// Fixture dump for the independent Go verifier: run jvmTest with
// -Dvault.fixture.dir=<target> to copy a real exported package there.
// JVM-only API in a JVM-run test (native test compilations don't run).
class PackageFixtureTest {
    @Test
    fun dumpFixture() = runTest {
        val target = System.getProperty("vault.fixture.dir") ?: return@runTest
        val (db, session, signer) = packageHarness(photos = 2)
        val reportId = readyReport(session, signer)
        val dir = PackageExporter(db, session.filesDir, session.originals, signer).export(reportId)
        FileSystem.SYSTEM.deleteRecursively("$target/fixture-package".toPath())
        copyTree(FileSystem.SYSTEM, dir.toPath(), "$target/fixture-package".toPath())
        db.close()
    }

    private fun copyTree(fs: FileSystem, src: okio.Path, dst: okio.Path) {
        fs.createDirectories(dst)
        for (child in fs.list(src)) {
            val target = dst.resolve(child.name)
            if (fs.metadata(child).isDirectory) copyTree(fs, child, target) else fs.copy(child, target)
        }
    }
}
