package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.*

internal val declarationTestRenderer = object : PlanRenderer {
    override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean) = "code-test-artifact".encodeToByteArray()
}

class ReportDeclarationsTest {
    @Test fun explicitIntentAppendAndFreshExportPreserveHistoricalBytes() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            val declarations = ReportDeclarations(db, session.filesDir, signer)
            val fs = FileSystem.SYSTEM
            val pdf = fs.read(generator.pdfPath(report.id).toPath()) { readByteArray() }
            val receipt = fs.read(generator.receiptPath(report.id).toPath()) { readByteArray() }
            assertTrue(declarations.check(report).isEmpty())
            assertFailsWith<IllegalArgumentException> { declarations.append(report.id, DeclarationIntent.AGREED, "", "", "", false) }
            assertFailsWith<IllegalArgumentException> { declarations.append(report.id, DeclarationIntent.DISAGREED, "", "", "", true) }
            val oldZip = generator.exportZip(report.id)
            val historical = importPackageZip(oldZip, session.filesDir)
            declarations.append(report.id, DeclarationIntent.AUTHOR, "", "", "", true)
            declarations.append(report.id, DeclarationIntent.DISAGREED, "Declared name", "tenant", "Wall condition disputed", true)
            assertContentEquals(pdf, fs.read(generator.pdfPath(report.id).toPath()) { readByteArray() })
            assertContentEquals(receipt, fs.read(generator.receiptPath(report.id).toPath()) { readByteArray() })
            val exported = generator.exportPackage(report.id)
            val verdict = PackageVerifier(signer).verify(exported)
            assertTrue(verdict.accepted())
            assertEquals(listOf(DeclarationIntent.AUTHOR, DeclarationIntent.DISAGREED), verdict.declarations.map { it.intent })
            assertTrue(verdict.declarations.all { it.valid && !it.trusted })
            assertTrue(PackageVerifier(signer).verify(historical).accepted())
            assertTrue(PackageVerifier(signer).verify(historical).declarations.isEmpty())
            val updated = importPackageZip(generator.exportZip(report.id), session.filesDir)
            assertEquals(2, PackageVerifier(signer).verify(updated).declarations.size)
            fs.deleteRecursively(historical.toPath()); fs.deleteRecursively(updated.toPath())
            val files = fs.list("$exported/declarations".toPath())
            fs.delete(files.first())
            assertFalse(PackageVerifier(signer).verify(exported).accepted())
            fs.deleteRecursively("$exported/declarations".toPath())
            fs.delete("$exported/declarations-index.json".toPath())
            assertFalse(PackageVerifier(signer).verify(exported).accepted())
        } finally { session.close() }
    }

    @Test fun alteredPrivateDeclarationBlocksExportAndReportDeletionRemovesCopies() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            val declarations = ReportDeclarations(db, session.filesDir, signer)
            declarations.append(report.id, DeclarationIntent.RECEIVED, "", "", "", true)
            val exported = generator.exportZip(report.id)
            val folder = declarations.directory(report.id).toPath()
            FileSystem.SYSTEM.write(FileSystem.SYSTEM.list(folder.resolve("declarations")).single()) { writeUtf8("altered") }
            assertFalse(generator.verify(report.id).declarationsIntact)
            assertFailsWith<IllegalArgumentException> { generator.exportZip(report.id) }
            generator.deleteReport(report.id)
            assertFalse(FileSystem.SYSTEM.exists(folder))
            assertFalse(FileSystem.SYSTEM.exists(exported.toPath()))
        } finally { session.close() }
    }
}
