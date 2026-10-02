package vault.local

import kotlinx.coroutines.test.runTest
import okio.*
import okio.Path.Companion.toPath
import kotlin.test.*

class PackageZipTest {
    @Test
    fun portableArchiveVerifiesWithoutOriginalDatabase() = runTest {
        val (db, session, signer) = sealedHarness(2)
        val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
        val renderer = object : PlanRenderer {
            override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
                onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean) = "%PDF-test".encodeToByteArray()
        }
        val report = generator.generate(db.inspectionDao().listAll().single().id, renderer)
        val zip = generator.exportZip(report.id)
        session.close()
        val imported = importPackageZip(zip, session.filesDir)
        val verdict = PackageVerifier(signer).verify(imported)
        assertTrue(verdict.accepted()); assertEquals(2, verdict.evidencesChecked)
        assertEquals(IssuerTrust.UNKNOWN, verdict.issuerTrust)
        val fs = FileSystem.SYSTEM
        val bytes = fs.read(zip.toPath()) { readByteArray() }
        bytes[50] = (bytes[50].toInt() xor 1).toByte()
        fs.write(zip.toPath()) { write(bytes) }
        assertFailsWith<IllegalArgumentException> { importPackageZip(zip, session.filesDir) }
    }

    @Test
    fun unsafeArchivePathNeverEscapesExtractionDirectory() {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-zip-traversal")
        fs.deleteRecursively(dir); fs.createDirectories(dir)
        val name = "../../escape.bin".encodeToByteArray()
        val archive = Buffer().writeIntLe(0x04034b50).writeShortLe(20).writeShortLe(0).writeShortLe(0)
            .writeIntLe(0).writeIntLe(0).writeIntLe(1).writeIntLe(1).writeShortLe(name.size).writeShortLe(0)
            .write(name).writeByte(0).readByteArray()
        val path = dir.resolve("bad.zip")
        fs.write(path) { write(archive) }
        assertFailsWith<IllegalArgumentException> { importPackageZip(path.toString(), dir.toString()) }
        assertFalse(fs.exists(dir.resolve("vault/escape.bin")))
    }

    @Test
    fun malformedManifestFailsClosed() {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-malformed-package")
        FileSystem.SYSTEM.deleteRecursively(dir); FileSystem.SYSTEM.createDirectories(dir)
        FileSystem.SYSTEM.write(dir.resolve("manifest.json")) { writeUtf8("{broken") }
        assertFalse(PackageVerifier(platformSigner(dir.toString())).verify(dir.toString()).accepted())
    }
}
