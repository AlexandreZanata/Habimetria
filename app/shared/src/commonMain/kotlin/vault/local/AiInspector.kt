package vault.local

import vault.findings.FindingPreset
import vault.local.db.AiSuggestion
import vault.local.db.LocalFinding
import vault.local.db.VaultDatabase

// Suggestion kinds. Hints only: the user confirms (creates a Finding)
// or ignores. Nothing here assigns liability or writes findings alone.
enum class SuggestionKind(val serial: String) {
    COVERAGE_GAP("coverage_gap"),
    POSSIBLE_STAIN("possible_stain"),
    POSSIBLE_CRACK("possible_crack");

    companion object {
        fun parse(raw: String): SuggestionKind? = entries.firstOrNull { it.serial == raw }
    }
}

enum class SuggestionStatus(val serial: String) {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    IGNORED("IGNORED");

    companion object {
        fun parse(raw: String): SuggestionStatus? = entries.firstOrNull { it.serial == raw }
    }
}

data class InspectorGap(
    val roomId: String,
    val roomLabel: String,
    val missing: List<ShotCategory>,
)

// Surfaces where a stain/crack review is worth the user's time. Walls,
// ceiling and floor carry the hint; overview/openings stay quiet to
// limit noise. Deterministic and offline: no model, no network.
fun inspectableCategories(): Set<ShotCategory> =
    setOf(ShotCategory.WALLS, ShotCategory.CEILING, ShotCategory.FLOOR)

class AiInspectorRepository(private val db: VaultDatabase) {

    // Idempotent: repeated runs never duplicate (unique index + IGNORE).
    // DRAFT-only; sealed inspections refuse new hints.
    suspend fun runInspection(inspectionId: String): List<AiSuggestion> {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) {
            throw SealedModificationException("inspection ${inspection.id} is ${inspection.status}")
        }
        for (room in db.roomDao().listForInspection(inspectionId)) {
            val applicable = applicableCategories(room.naCategories)
            val captured = db.evidenceDao().listForRoom(room.id)
                .mapNotNull { ShotCategory.parse(it.shotCategory) }
                .filter { it in applicable }
                .toSet()
            for (missing in applicable.filter { it !in captured }) {
                insertHint(
                    inspectionId = inspectionId,
                    roomId = room.id,
                    evidenceId = "",
                    kind = SuggestionKind.COVERAGE_GAP,
                    detail = missing.name,
                )
            }
        }
        val findingsByEvidence = db.evidenceDao().listForInspection(inspectionId)
            .associate { it.id to db.findingDao().listForEvidence(it.id).isNotEmpty() }
        for (evidence in db.evidenceDao().listForInspection(inspectionId)) {
            val category = ShotCategory.parse(evidence.shotCategory) ?: continue
            if (category !in inspectableCategories()) continue
            if (findingsByEvidence[evidence.id] == true) continue
            val roomId = evidence.roomId ?: continue
            insertHint(
                inspectionId = inspectionId,
                roomId = roomId,
                evidenceId = evidence.id,
                kind = SuggestionKind.POSSIBLE_STAIN,
                detail = "",
            )
            insertHint(
                inspectionId = inspectionId,
                roomId = roomId,
                evidenceId = evidence.id,
                kind = SuggestionKind.POSSIBLE_CRACK,
                detail = "",
            )
        }
        return db.aiSuggestionDao().listForInspection(inspectionId)
    }

    suspend fun coverageGaps(inspectionId: String): List<InspectorGap> {
        val gaps = mutableListOf<InspectorGap>()
        for (room in db.roomDao().listForInspection(inspectionId)) {
            val applicable = applicableCategories(room.naCategories)
            val captured = db.evidenceDao().listForRoom(room.id)
                .mapNotNull { ShotCategory.parse(it.shotCategory) }
                .filter { it in applicable }
                .toSet()
            val missing = applicable.filter { it !in captured }
            if (missing.isNotEmpty()) gaps += InspectorGap(room.id, room.label, missing)
        }
        return gaps
    }

    suspend fun pendingForInspection(inspectionId: String): List<AiSuggestion> =
        db.aiSuggestionDao().listPending(inspectionId)

    suspend fun listForInspection(inspectionId: String): List<AiSuggestion> =
        db.aiSuggestionDao().listForInspection(inspectionId)

    // Confirm creates the Finding; the hint flips to CONFIRMED. Gaps have
    // no finding: capture the missing photo instead.
    suspend fun confirm(suggestionId: String, note: String? = null): LocalFinding {
        val hint = db.aiSuggestionDao().get(suggestionId)
            ?: throw LocalValidationException("suggestion not found")
        if (hint.status != SuggestionStatus.PENDING.serial) {
            throw LocalValidationException("suggestion already decided")
        }
        val preset = when (hint.kind) {
            SuggestionKind.POSSIBLE_STAIN.serial -> FindingPreset.STAIN
            SuggestionKind.POSSIBLE_CRACK.serial -> FindingPreset.CRACK
            else -> throw LocalValidationException("coverage gap has no finding; capture the missing photo")
        }
        val room = db.roomDao().get(hint.roomId)
            ?: throw LocalValidationException("room not found")
        val inspection = db.inspectionDao().get(hint.inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) {
            throw SealedModificationException("inspection ${inspection.id} is ${inspection.status}")
        }
        val evidence = db.evidenceDao().get(hint.evidenceId)
            ?: throw LocalValidationException("evidence not found")
        if (evidence.inspectionId != room.inspectionId) {
            throw LocalValidationException("evidence belongs to another inspection")
        }
        val row = LocalFinding(
            id = newLocalId("find"), roomId = room.id, evidenceId = evidence.id,
            preset = preset.name, note = note?.takeIf { it.isNotBlank() },
        )
        db.findingDao().insert(row)
        db.aiSuggestionDao().update(hint.copy(status = SuggestionStatus.CONFIRMED.serial))
        return row
    }

    suspend fun ignore(suggestionId: String): AiSuggestion {
        val hint = db.aiSuggestionDao().get(suggestionId)
            ?: throw LocalValidationException("suggestion not found")
        if (hint.status != SuggestionStatus.PENDING.serial) {
            throw LocalValidationException("suggestion already decided")
        }
        val inspection = db.inspectionDao().get(hint.inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) {
            throw SealedModificationException("inspection ${inspection.id} is ${inspection.status}")
        }
        val updated = hint.copy(status = SuggestionStatus.IGNORED.serial)
        db.aiSuggestionDao().update(updated)
        return updated
    }

    private suspend fun insertHint(
        inspectionId: String,
        roomId: String,
        evidenceId: String,
        kind: SuggestionKind,
        detail: String,
    ) {
        val row = AiSuggestion(
            id = newLocalId("sug"), inspectionId = inspectionId, roomId = roomId,
            evidenceId = evidenceId, kind = kind.serial, detail = detail,
            status = SuggestionStatus.PENDING.serial, createdAtDevice = deviceNowIso(),
        )
        db.aiSuggestionDao().insert(row)
    }
}
