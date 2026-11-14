package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalRoom
import kotlin.test.assertEquals

internal data class FreeLocalGraph(
    val propertyId: String,
    val tenancyId: String,
    val inspectionId: String,
    val rooms: List<LocalRoom>,
    val evidenceIds: List<String>,
    val occurrenceId: String,
)

// Real file-backed repositories and installation signing; no account or billing
// adapter is needed. Unique directories are removed even when assertions fail.
internal class FreeLocalFixture(val access: FreeLocalAccess) {
    val fs = FileSystem.SYSTEM
    val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(newLocalId("free-local")).toString()
    var session = openLocalSession(directory)
        private set
    val signer = platformSigner(directory)
    val network = FreeLocalNetwork(access)

    suspend fun seed(): FreeLocalGraph {
        val repo = session.repository
        val property = repo.createPropertyStructured("Rua Sintética", "42", null, "Centro", "Fixture")
        val tenancy = repo.createTenancy(property.id)
        val inspection = repo.createInspection(tenancy.id)
        val rooms = listOf(repo.addRoom(inspection.id, "Suíte çãõ"), repo.addRoom(inspection.id, "Garagem"))
        val captures = mutableListOf<LocalEvidence>()
        // Repeated photos are not a subscription quota or distinct coverage.
        repeat(12) { captures += capture(inspection.id, rooms[0].id, ShotCategory.OVERVIEW) }
        for (category in ShotCategory.entries.filter { it != ShotCategory.OVERVIEW }) {
            captures += capture(inspection.id, rooms[0].id, category)
        }
        captures += capture(inspection.id, rooms[1].id, ShotCategory.OVERVIEW)
        repo.setRoomNotApplicable(rooms[1].id, ShotCategory.entries.filter { it != ShotCategory.OVERVIEW }.toSet())
        repo.addFinding(rooms[0].id, captures.first().id, FindingPreset.CRACK, "Synthetic review note")
        ReportDetailsRepository(session.db).save(inspection.id, ReportDetails(
            purpose = "Synthetic local inspection", scope = "Photographic record", photoLayout = "detail",
        ))
        session.navigation.save(NavigationProgress(inspection.id, rooms[0].id))
        val occurrence = session.occurrences.createOccurrence(property.id, "Synthetic occurrence", null)
        network.rememberHostedLink(session, property.id)
        return FreeLocalGraph(property.id, tenancy.id, inspection.id, rooms, captures.map { it.id }, occurrence.id)
    }

    suspend fun capture(inspectionId: String, roomId: String, category: ShotCategory): LocalEvidence {
        val id = newLocalId("ev")
        val stored = session.originals.storeOriginal(id, pdfTestPhoto())
        return session.repository.recordEvidence(
            inspectionId, roomId, "$id.bin", "image/jpeg", stored.size, stored.sha256,
            shotCategory = category, evidenceId = id, capturedAtDevice = "2026-01-02T03:04:05Z",
        )
    }

    suspend fun originalHashes(inspectionId: String): Map<String, String> =
        session.repository.evidencesFor(inspectionId).associate { evidence ->
            val actual = session.originals.sha256Of(session.originals.readOriginal(evidence.id))
            assertEquals(evidence.sha256, actual)
            evidence.id to actual
        }

    fun sealer() = LocalSealer(session.db, session.originals, signer)

    fun generator() = ReportGenerator(session.db, directory, session.originals, session.derivatives, signer)

    fun reopen() {
        session.close()
        session = openLocalSession(directory)
    }

    fun close() {
        try { session.close() } finally {
            network.close()
            fs.deleteRecursively(directory.toPath())
        }
    }
}
