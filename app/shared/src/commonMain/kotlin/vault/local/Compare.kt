package vault.local

import vault.local.db.Comparison
import vault.local.db.LocalEvidence
import vault.local.db.VaultDatabase

// Visual-state tags only. Never liability, never automated: the user
// confirms every tag. V1.4 may *suggest*; only the user decides.
enum class CompareTag(val serial: String) {
    NO_CHANGE("no_change"),
    CHANGED("changed"),
    WEAR("wear");

    companion object {
        fun parse(raw: String): CompareTag? = entries.firstOrNull { it.serial == raw }
    }
}

data class PositionPair(
    val entryRoomId: String,
    val exitRoomId: String,
    val roomLabel: String,
    val category: ShotCategory,
    val entryEvidence: LocalEvidence?,
    val exitEvidence: LocalEvidence?,
    val tag: CompareTag?,
)

class CompareRepository(private val db: VaultDatabase) {

    // Pairs every applicable exit position with its entry counterpart.
    // Latest photo per side wins; N/A positions are excluded on the
    // exit side; rooms without an entry link are skipped.
    suspend fun pairsForExit(exitInspectionId: String): List<PositionPair> {
        val exit = db.inspectionDao().get(exitInspectionId)
            ?: throw LocalValidationException("inspection not found")
        val entryId = entryInspectionOf(exit)
        val exitRooms = db.roomDao().listForInspection(exit.id)
        return exitRooms.flatMap { exitRoom ->
            val entryRoomId = exitRoom.sourceRoomId ?: return@flatMap emptyList()
            val applicable = applicableCategories(exitRoom.naCategories)
            val entryLatest = latestByCategory(entryId, entryRoomId)
            val exitLatest = latestByCategory(exit.id, exitRoom.id)
            applicable.map { category ->
                PositionPair(
                    entryRoomId = entryRoomId,
                    exitRoomId = exitRoom.id,
                    roomLabel = exitRoom.label,
                    category = category,
                    entryEvidence = entryLatest[category],
                    exitEvidence = exitLatest[category],
                    tag = db.comparisonDao().get(exitRoom.id, category.name)?.let { CompareTag.parse(it.tag) },
                )
            }
        }
    }

    private suspend fun entryInspectionOf(exit: vault.local.db.LocalInspection): String {
        val tenancyRooms = db.inspectionDao().listForTenancy(exit.tenancyId)
        return tenancyRooms.firstOrNull { it.kind == "entry" }?.id
            ?: throw LocalValidationException("entry inspection not found")
    }

    private suspend fun latestByCategory(
        inspectionId: String,
        roomId: String,
    ): Map<ShotCategory, LocalEvidence> {
        val result = mutableMapOf<ShotCategory, LocalEvidence>()
        for (evidence in db.evidenceDao().listForRoom(roomId)) {
            if (evidence.inspectionId != inspectionId) continue
            val category = ShotCategory.parse(evidence.shotCategory) ?: continue
            val current = result[category]
            if (current == null || evidence.sequence > current.sequence) result[category] = evidence
        }
        return result
    }

    suspend fun setTag(
        tenancyId: String,
        entryRoomId: String,
        exitRoomId: String,
        category: ShotCategory,
        tag: CompareTag,
        entryEvidenceId: String? = null,
        exitEvidenceId: String? = null,
    ): Comparison {
        val row = Comparison(
            id = newLocalId("cmp"), tenancyId = tenancyId,
            entryRoomId = entryRoomId, exitRoomId = exitRoomId,
            shotCategory = category.name, entryEvidenceId = entryEvidenceId,
            exitEvidenceId = exitEvidenceId, tag = tag.serial,
            createdAtDevice = deviceNowIso(),
        )
        db.comparisonDao().upsert(row)
        return row
    }

    suspend fun tagsForTenancy(tenancyId: String): List<Comparison> =
        db.comparisonDao().listForTenancy(tenancyId)
}
