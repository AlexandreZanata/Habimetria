package vault.local

import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase

interface RoomsOperations {
    suspend fun addRoom(inspectionId: String, label: String, sortHint: Int? = null): LocalRoom

    suspend fun renameRoom(roomId: String, label: String): LocalRoom

    suspend fun deleteRoom(roomId: String, allowWithEvidence: Boolean = false)

    suspend fun moveRoom(roomId: String, newIndex: Int): List<LocalRoom>

    suspend fun setRoomNotApplicable(roomId: String, na: Set<ShotCategory>): LocalRoom

    suspend fun roomsFor(inspectionId: String): List<LocalRoom>
}

internal class LocalRooms(private val db: VaultDatabase) : RoomsOperations {
    private val guard = LocalWriteGuard(db)

    override suspend fun addRoom(inspectionId: String, label: String, sortHint: Int?): LocalRoom = localWrite {
        val inspection = guard.requireDraft(inspectionId)
        val clean = guard.validLabel(label)
        val existing = db.roomDao().listForInspection(inspection.id)
        val row = LocalRoom(
            id = newLocalId("room"),
            inspectionId = inspection.id,
            roomKey = "room_" + newLocalId("k").substringAfter("_"),
            label = clean,
            sort = sortHint ?: existing.size,
            naCategories = "",
        )
        db.roomDao().insert(row)
        return@localWrite row
    }

    override suspend fun renameRoom(roomId: String, label: String): LocalRoom = localWrite {
        val room = guard.requireRoom(roomId)
        guard.requireDraft(room.inspectionId)
        val updated = room.copy(label = guard.validLabel(label))
        db.roomDao().update(updated)
        return@localWrite updated
    }

    // Deleting a room with evidence requires an explicit decision:
    // pass allowWithEvidence = true to confirm. Frozen when sealed.

    override suspend fun deleteRoom(roomId: String, allowWithEvidence: Boolean) = localWrite {
        val room = guard.requireRoom(roomId)
        guard.requireDraft(room.inspectionId)
        val attached = db.evidenceDao().listForRoom(roomId)
        if (attached.isNotEmpty() && !allowWithEvidence) {
            throw LocalValidationException("room has ${attached.size} evidence rows; confirm deletion")
        }
        if (attached.isNotEmpty()) {
            throw LocalValidationException("room has evidence; move or remove it first")
        }
        db.roomDao().delete(roomId)
    }

    // Reordering is DRAFT-only and renormalizes sort to 0..n-1.
    // Out-of-range targets clamp; sealed inspections refuse.

    override suspend fun moveRoom(roomId: String, newIndex: Int): List<LocalRoom> = localWrite {
        val room = guard.requireRoom(roomId)
        guard.requireDraft(room.inspectionId)
        val rooms = db.roomDao().listForInspection(room.inspectionId).toMutableList()
        rooms.removeAll { it.id == roomId }
        rooms.add(newIndex.coerceIn(0, rooms.size), room)
        rooms.forEachIndexed { index, current ->
            if (current.sort != index) db.roomDao().update(current.copy(sort = index))
        }
        return@localWrite db.roomDao().listForInspection(room.inspectionId)
    }

    override suspend fun setRoomNotApplicable(roomId: String, na: Set<ShotCategory>): LocalRoom = localWrite {
        val room = guard.requireRoom(roomId)
        guard.requireDraft(room.inspectionId)
        val updated = room.copy(naCategories = na.joinToString(",") { it.name })
        db.roomDao().update(updated)
        return@localWrite updated
    }

    // Production flow stores the original first (size+sha known), then
    // records the row with the same id so DB and files stay bound.
    // Location and EXIF ride along as device claims; a null claim keeps
    // the row explicitly without location.

    override suspend fun roomsFor(inspectionId: String): List<LocalRoom> =
        db.roomDao().listForInspection(inspectionId)
}
