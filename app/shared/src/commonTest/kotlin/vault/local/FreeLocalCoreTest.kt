package vault.local

import kotlinx.coroutines.test.runTest
import vault.findings.FindingPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreeLocalCoreTest {
    @Test fun neverRegisteredCore() = runTest { exerciseCore(FreeLocalAccess.NEVER_REGISTERED) }
    @Test fun expiredHostedAccessCore() = runTest { exerciseCore(FreeLocalAccess.EXPIRED_HOSTED_ACCESS) }
    @Test fun offlineCore() = runTest { exerciseCore(FreeLocalAccess.OFFLINE) }

    private suspend fun exerciseCore(access: FreeLocalAccess) {
        val fixture = FreeLocalFixture(access)
        try {
            assertFalse(fixture.session.startup().reconcile.hasProblems())
            val graph = fixture.seed()
            val originals = fixture.originalHashes(graph.inspectionId)
            // A fixed synthetic JPEG digest detects accidental fixture changes.
            assertEquals(setOf("297ee6a85d5da81e94d05598eaf190e9c1519179c4ebd01f616c7000d911ae94"), originals.values.toSet())
            assertEquals(17, originals.size)
            fixture.reopen()
            val session = fixture.session
            val repo = session.repository
            val startup = session.startup()
            assertFalse(startup.reconcile.hasProblems())
            assertEquals(17, startup.reconcile.checked)
            assertEquals(NavigationProgress(graph.inspectionId, graph.rooms[0].id), session.navigation.load())
            assertEquals("Fixture", repo.propertyFor(graph.inspectionId)!!.nickname)
            assertEquals(listOf("Suíte çãõ", "Garagem"), repo.roomsFor(graph.inspectionId).map { it.label })
            assertEquals((1..17).toList(), repo.evidencesFor(graph.inspectionId).map { it.sequence })
            assertEquals(1, repo.findingsForRoom(graph.rooms[0].id).size)
            assertEquals("detail", ReportDetailsRepository(session.db).get(graph.inspectionId).photoLayout)
            for (room in graph.rooms) assertEquals(100, session.coverage.roomCoverage(room.id).percent())
            fixture.network.assertNoTraffic(session, graph)

            // Safety validation still applies to the free path.
            assertFailsWith<LocalValidationException> { repo.addRoom(graph.inspectionId, " ") }
            assertFailsWith<LocalValidationException> { repo.addRoom(graph.inspectionId, "x".repeat(MAX_ROOM_LABEL + 1)) }
            assertFailsWith<LocalStoreException> {
                session.originals.storeOriginal(graph.evidenceIds.first(), pdfTestPhoto())
            }
            val assessment = fixture.sealer().assess(graph.inspectionId)
            assertTrue(assessment.canSeal)
            assertEquals(17, assessment.evidences)
            val manifest = (fixture.sealer().seal(graph.inspectionId) as SealOutcome.Sealed).manifest
            assertTrue(fixture.sealer().verifyManifest(manifest, fixture.signer.publicKeyUncompressed()))
            fixture.reopen()
            val resumed = fixture.session.repository
            assertEquals(STATUS_SEALED, resumed.getInspection(graph.inspectionId)!!.status)
            assertEquals(manifest, fixture.session.db.manifestDao().getByInspection(graph.inspectionId))
            assertTrue(fixture.sealer().seal(graph.inspectionId) is SealOutcome.AlreadySealed)
            assertFailsWith<SealedModificationException> { resumed.renameRoom(graph.rooms[0].id, "Changed") }
            assertFailsWith<SealedModificationException> {
                resumed.addFinding(graph.rooms[0].id, graph.evidenceIds.first(), FindingPreset.STAIN)
            }
            assertFailsWith<SealedModificationException> { fixture.session.deleteEvidence(graph.evidenceIds.first()) }
            assertEquals(originals, fixture.originalHashes(graph.inspectionId))
            assertEquals(manifest, fixture.session.db.manifestDao().getByInspection(graph.inspectionId))
            fixture.network.assertNoTraffic(fixture.session, graph)
        } finally { fixture.close() }
    }
}
