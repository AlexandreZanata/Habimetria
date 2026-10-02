package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.*

class DeclarationRecoveryTest {
    @Test fun interruptedCommitRecoversOnlyIndexBoundPendingBytes() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val report = generator.generate(db.inspectionDao().listAll().single().id, declarationTestRenderer)
            var failMove = true
            val fs = object : ForwardingFileSystem(FileSystem.SYSTEM) {
                override fun atomicMove(source: Path, target: Path) {
                    if (failMove && "/pending/" in source.toString() && target.parent?.name == "declarations") {
                        failMove = false; error("injected commit interruption")
                    }
                    super.atomicMove(source, target)
                }
            }
            val declarations = ReportDeclarations(db, session.filesDir, signer, fs)
            assertFailsWith<IllegalStateException> { declarations.append(report.id, DeclarationIntent.AUTHOR, "", "", "", true) }
            assertEquals(DeclarationIntent.AUTHOR, declarations.check(report).single().intent)
            assertTrue(generator.verify(report.id).allOk())
            val folder = declarations.directory(report.id).toPath()
            val committed = fs.list(folder.resolve("declarations")).single()
            fs.createDirectories(folder.resolve("pending"))
            val pending = folder.resolve("pending/${committed.name}")
            fs.atomicMove(committed,pending)
            fs.write(pending) { writeUtf8("different pending content") }
            assertFailsWith<IllegalArgumentException> { declarations.check(report) }
            assertFalse(generator.verify(report.id).allOk())
            assertFalse(fs.exists(committed))
        } finally { session.close() }
    }

    @Test fun declarationIndexCannotBeTransplantedToNewPdfVersion() = runTest {
        val (db, session, signer) = sealedHarness(1)
        try {
            val generator = ReportGenerator(db, session.filesDir, session.originals, session.derivatives, signer)
            val inspection = db.inspectionDao().listAll().single().id
            val first = generator.generate(inspection, declarationTestRenderer)
            val second = generator.generate(inspection, declarationTestRenderer, newVersion = true)
            val a = generator.exportPackage(first.id)
            val b = generator.exportPackage(second.id)
            FileSystem.SYSTEM.copy("$a/declarations-index.json".toPath(), "$b/declarations-index.json".toPath())
            assertFalse(PackageVerifier(signer).verify(b).accepted())
        } finally { session.close() }
    }
}
