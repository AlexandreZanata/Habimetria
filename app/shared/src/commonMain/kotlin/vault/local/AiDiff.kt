package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.VisualDiff
import vault.local.db.VaultDatabase

// Offline heuristic signals for one entry-vs-exit position. Hints only:
// "visual difference found — compare the photos". Never liability,
// never automated tagging; the user reviews every row.
enum class DiffSignal(val serial: String) {
    PRESENCE_CHANGE("presence_change"),
    FINDINGS_CHANGED("findings_changed"),
    USER_TAGGED("user_tagged"),
    LOCATION_CHANGED("location_changed"),
    BYTES_DIFFER("bytes_differ");

    companion object {
        fun parse(raw: String): DiffSignal? = entries.firstOrNull { it.serial == raw }
    }
}

enum class DiffStatus(val serial: String) {
    PENDING("PENDING"),
    REVIEWED("REVIEWED"),
    DISMISSED("DISMISSED");

    companion object {
        fun parse(raw: String): DiffStatus? = entries.firstOrNull { it.serial == raw }
    }
}

fun parseDiffSignals(raw: String): List<DiffSignal> =
    raw.split(",").mapNotNull { DiffSignal.parse(it.trim().takeIf { s -> s.isNotEmpty() } ?: "") }

class VisualDiffRepository(private val db: VaultDatabase) {
    private val compare = CompareRepository(db)

    // Idempotent per position (unique index + IGNORE). Pending rows get
    // fresh signals on retakes; decided rows keep their history.
    // Read-only on inspections: safe to run on DRAFT or SEALED exits.
    suspend fun runDiff(exitInspectionId: String): List<VisualDiff> {
        val exit = db.inspectionDao().get(exitInspectionId)
            ?: throw LocalValidationException("inspection not found")
        for (pair in compare.pairsForExit(exitInspectionId)) {
            val signals = signalsFor(pair)
            // Byte difference alone never raises a row: any two captures
            // differ at byte level. Presence, findings, user tag or
            // location changes do.
            if (signals.none { it != DiffSignal.BYTES_DIFFER }) continue
            val existing = db.visualDiffDao().get(pair.exitRoomId, pair.category.name)
            if (existing == null) {
                db.visualDiffDao().insert(
                    VisualDiff(
                        id = newLocalId("dif"), tenancyId = exit.tenancyId ?: throw LocalValidationException("tenancy required"),
                        exitInspectionId = exitInspectionId,
                        entryRoomId = pair.entryRoomId, exitRoomId = pair.exitRoomId,
                        shotCategory = pair.category.name,
                        entryEvidenceId = pair.entryEvidence?.id.orEmpty(),
                        exitEvidenceId = pair.exitEvidence?.id.orEmpty(),
                        signals = signals.joinToString(",") { it.serial },
                        status = DiffStatus.PENDING.serial,
                        createdAtDevice = deviceNowIso(),
                    ),
                )
            } else if (existing.status == DiffStatus.PENDING.serial) {
                db.visualDiffDao().update(
                    existing.copy(
                        entryEvidenceId = pair.entryEvidence?.id.orEmpty(),
                        exitEvidenceId = pair.exitEvidence?.id.orEmpty(),
                        signals = signals.joinToString(",") { it.serial },
                    ),
                )
            }
        }
        return db.visualDiffDao().listForExit(exitInspectionId)
    }

    suspend fun pendingForExit(exitInspectionId: String): List<VisualDiff> =
        db.visualDiffDao().listPending(exitInspectionId)

    suspend fun listForExit(exitInspectionId: String): List<VisualDiff> =
        db.visualDiffDao().listForExit(exitInspectionId)

    suspend fun markReviewed(diffId: String): VisualDiff = decide(diffId, DiffStatus.REVIEWED)

    suspend fun dismiss(diffId: String): VisualDiff = decide(diffId, DiffStatus.DISMISSED)

    private suspend fun decide(diffId: String, status: DiffStatus): VisualDiff {
        val row = db.visualDiffDao().getById(diffId)
            ?: throw LocalValidationException("diff not found")
        if (row.status != DiffStatus.PENDING.serial) {
            throw LocalValidationException("diff already decided")
        }
        val updated = row.copy(status = status.serial)
        db.visualDiffDao().update(updated)
        return updated
    }

    private suspend fun signalsFor(pair: PositionPair): List<DiffSignal> {
        val signals = mutableListOf<DiffSignal>()
        val entry = pair.entryEvidence
        val exit = pair.exitEvidence
        if ((entry == null) != (exit == null)) signals += DiffSignal.PRESENCE_CHANGE
        if (findingsCount(entry) != findingsCount(exit)) signals += DiffSignal.FINDINGS_CHANGED
        if (pair.tag == CompareTag.CHANGED || pair.tag == CompareTag.WEAR) {
            signals += DiffSignal.USER_TAGGED
        }
        if (entry != null && exit != null) {
            if (locationChanged(entry, exit)) signals += DiffSignal.LOCATION_CHANGED
            if (entry.sha256 != exit.sha256) signals += DiffSignal.BYTES_DIFFER
        }
        return signals
    }

    private suspend fun findingsCount(evidence: LocalEvidence?): Int {
        if (evidence == null) return 0
        return db.findingDao().listForEvidence(evidence.id).size
    }

    private fun locationChanged(entry: LocalEvidence, exit: LocalEvidence): Boolean =
        entry.locationStatus != exit.locationStatus || entry.isMocked != exit.isMocked
}
