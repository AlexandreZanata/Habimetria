package vault.local

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FreeLocalJourneyTest {
    @Test fun neverRegisteredJourney() = runTest { exerciseJourney(FreeLocalAccess.NEVER_REGISTERED) }
    @Test fun expiredHostedAccessJourney() = runTest { exerciseJourney(FreeLocalAccess.EXPIRED_HOSTED_ACCESS) }
    @Test fun offlineJourney() = runTest { exerciseJourney(FreeLocalAccess.OFFLINE) }

    private suspend fun exerciseJourney(access: FreeLocalAccess) {
        val fixture = FreeLocalFixture(access)
        try {
            val graph = fixture.seed()
            reviewWithAssistant(fixture, graph)
            val originals = fixture.originalHashes(graph.inspectionId)
            val manifest = (fixture.sealer().seal(graph.inspectionId) as SealOutcome.Sealed).manifest
            assertEquals(2, manifestSnapshot(manifest).findings.size)
            val generator = fixture.generator()
            val renderer = FreeLocalPlanRenderer()
            val progress = mutableListOf<Pair<Int, Int>>()
            val report = generator.generate(graph.inspectionId, renderer, { done, total -> progress += done to total })
            assertEquals(REPORT_READY, report.status)
            assertEquals(graph.evidenceIds, renderer.photoIds)
            assertTrue(renderer.text.any { it.contains("Synthetic local inspection") })
            assertTrue(progress.isNotEmpty() && progress.last().first == progress.last().second)
            assertTrue(generator.verify(report.id).allOk())
            val pdf = fixture.fs.read(generator.pdfPath(report.id).toPath()) { readByteArray() }
            val receipt = fixture.fs.read(generator.receiptPath(report.id).toPath()) { readByteArray() }
            val historical = importPackageZip(generator.exportZip(report.id), fixture.directory)
            val declarations = ReportDeclarations(fixture.session.db, fixture.directory, fixture.signer)
            assertTrue(declarations.check(report).isEmpty())
            for (intent in DeclarationIntent.entries) {
                declarations.append(report.id, intent, "", "", "Synthetic explicit intent", true)
            }
            val current = importPackageZip(generator.exportZip(report.id), fixture.directory)
            // A fresh recipient installation needs no account, original DB or
            // issuer private key. An included key cannot grant issuer trust.
            val verifier = PackageVerifier(platformSigner("${fixture.directory}/recipient"))
            val verdict = verifier.verify(current)
            assertTrue(verdict.accepted())
            assertEquals(IssuerTrust.UNKNOWN, verdict.issuerTrust)
            assertEquals(17, verdict.evidencesChecked)
            assertEquals(DeclarationIntent.entries, verdict.declarations.map { it.intent })
            assertTrue(verdict.declarations.all { it.valid && !it.trusted })
            val pinned = verifier.verify(current, pinnedKeys = setOf(fixture.signer.keyId))
            assertEquals(IssuerTrust.PINNED, pinned.issuerTrust)
            assertTrue(pinned.declarations.all { it.valid && it.trusted })
            assertTrue(verifier.verify(historical).accepted())
            assertTrue(verifier.verify(historical).declarations.isEmpty())
            assertContentEquals(pdf, fixture.fs.read("$current/report.pdf".toPath()) { readByteArray() })
            assertContentEquals(manifest.canonicalJson.encodeToByteArray(),
                fixture.fs.read("$current/manifest.json".toPath()) { readByteArray() })

            compareWithExit(fixture, graph)
            val version = generator.generate(graph.inspectionId, FreeLocalPlanRenderer(), newVersion = true)
            assertNotEquals(report.id, version.id)
            assertEquals(2, version.versionNumber)
            assertEquals(listOf(1, 2), generator.versions(manifest.id).map { it.versionNumber }.sorted())
            assertEquals(version.id, generator.generate(graph.inspectionId, renderer).id)
            assertEquals(1, renderer.calls)
            fixture.reopen()
            assertFalse(fixture.session.startup().reconcile.hasProblems())
            assertEquals(CompareTag.CHANGED.serial, fixture.session.compare.tagsForTenancy(graph.tenancyId).single().tag)
            val decisions = fixture.session.inspector.listForInspection(graph.inspectionId)
            assertEquals(1, decisions.count { it.status == SuggestionStatus.CONFIRMED.serial })
            assertEquals(1, decisions.count { it.status == SuggestionStatus.IGNORED.serial })
            assertTrue(fixture.generator().verify(report.id).allOk())
            assertTrue(fixture.generator().verify(version.id).allOk())
            assertEquals(manifest, fixture.session.db.manifestDao().getByInspection(graph.inspectionId))
            assertEquals(originals, fixture.originalHashes(graph.inspectionId))
            assertContentEquals(pdf, fixture.fs.read(generator.pdfPath(report.id).toPath()) { readByteArray() })
            assertContentEquals(receipt, fixture.fs.read(generator.receiptPath(report.id).toPath()) { readByteArray() })
            assertFailsWith<SealedModificationException> { fixture.session.inspector.runInspection(graph.inspectionId) }
            fixture.network.assertNoTraffic(fixture.session, graph)

            // Free import/export retains all integrity and size/path defenses.
            // Equal-length changes must fail hashes, not merely file sizes.
            val originalPath = "$current/originals/${graph.evidenceIds.first()}.bin".toPath()
            val changed = fixture.fs.read(originalPath) { readByteArray() }
            changed[changed.lastIndex] = (changed.last().toInt() xor 1).toByte()
            fixture.fs.write(originalPath) { write(changed) }
            assertEquals(listOf(graph.evidenceIds.first()), verifier.verify(current).evidencesModified)
            assertEquals(FileStatus.OK, verifier.verify(current).pdf)
            assertTrue(verifier.verify(current).signatureValid)
            assertFalse(verifier.verify(current).accepted())
            assertTrue(verifier.verify(historical).accepted())
            fixture.fs.write(fixture.session.originals.originalPath(graph.evidenceIds.first()).toPath()) { write(changed) }
            val damaged = fixture.generator().verify(report.id)
            assertTrue(damaged.pdfIntact && damaged.manifestOk && damaged.receiptOk)
            assertFalse(damaged.originalsIntact)
            assertFailsWith<IllegalArgumentException> { fixture.generator().exportZip(report.id) }
            assertEquals(manifest, fixture.session.db.manifestDao().getByInspection(graph.inspectionId))
            fixture.network.assertNoTraffic(fixture.session, graph)
        } finally { fixture.close() }
    }

    private suspend fun reviewWithAssistant(fixture: FreeLocalFixture, graph: FreeLocalGraph) {
        val session = fixture.session
        val before = graph.rooms.sumOf { session.repository.findingsForRoom(it.id).size }
        val hints = session.inspector.runInspection(graph.inspectionId)
        assertEquals(hints.size, session.inspector.runInspection(graph.inspectionId).size)
        assertTrue(session.inspector.coverageGaps(graph.inspectionId).isEmpty())
        assertEquals(before, graph.rooms.sumOf { session.repository.findingsForRoom(it.id).size })
        val stain = hints.first { it.kind == SuggestionKind.POSSIBLE_STAIN.serial }
        val crack = hints.first { it.kind == SuggestionKind.POSSIBLE_CRACK.serial }
        assertEquals("STAIN", session.inspector.confirm(stain.id, "Synthetic confirmed observation").preset)
        assertEquals(SuggestionStatus.IGNORED.serial, session.inspector.ignore(crack.id).status)
        assertEquals(before + 1, graph.rooms.sumOf { session.repository.findingsForRoom(it.id).size })
    }

    private suspend fun compareWithExit(fixture: FreeLocalFixture, graph: FreeLocalGraph) {
        val session = fixture.session
        val exit = session.repository.startExitInspection(graph.tenancyId)
        val room = session.repository.roomsFor(exit.id).first()
        val evidence = fixture.capture(exit.id, room.id, ShotCategory.OVERVIEW)
        val pair = session.compare.pairsForExit(exit.id).first { it.exitRoomId == room.id && it.category == ShotCategory.OVERVIEW }
        assertEquals(graph.evidenceIds[11], pair.entryEvidence?.id)
        assertEquals(evidence.id, pair.exitEvidence?.id)
        session.compare.setTag(graph.tenancyId, pair.entryRoomId, room.id, pair.category,
            CompareTag.CHANGED, pair.entryEvidence?.id, evidence.id)
        assertEquals(CompareTag.CHANGED, session.compare.pairsForExit(exit.id).first().tag)
        fixture.sealer().seal(exit.id)
        fixture.network.assertNoTraffic(session, graph, exit.id)
    }
}
