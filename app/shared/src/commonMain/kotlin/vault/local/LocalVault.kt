package vault.local

import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase

class SealedModificationException(message: String) : IllegalStateException(message)
class LocalValidationException(message: String) : IllegalArgumentException(message)

const val MAX_ROOM_LABEL = 80
const val STATUS_DRAFT = "DRAFT"
const val STATUS_SEALED = "SEALED"

// LocalVaultRepository is the offline application core: every capture,
// room and finding flows through here. No network, no VaultApi.
class LocalVaultRepository(private val db: VaultDatabase) {

    suspend fun createProperty(address: String): LocalProperty {
        val clean = address.trim()
        if (clean.length < 3) throw LocalValidationException("address too short")
        val row = LocalProperty(newLocalId("prop"), clean, deviceNowIso())
        db.propertyDao().insert(row)
        return row
    }

    suspend fun createTenancy(propertyId: String, type: String = "entry"): LocalTenancy {
        requireProperty(propertyId)
        val row = LocalTenancy(newLocalId("ten"), propertyId, type, deviceNowIso())
        db.tenancyDao().insert(row)
        return row
    }

    suspend fun createInspection(tenancyId: String, kind: String = "entry"): LocalInspection {
        requireTenancy(tenancyId)
        val row = LocalInspection(newLocalId("ins"), tenancyId, kind, STATUS_DRAFT, deviceNowIso(), null)
        db.inspectionDao().insert(row)
        return row
    }

    suspend fun addRoom(inspectionId: String, label: String, sortHint: Int? = null): LocalRoom {
        val inspection = requireDraft(inspectionId)
        val clean = validLabel(label)
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
        return row
    }

    suspend fun renameRoom(roomId: String, label: String): LocalRoom {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val updated = room.copy(label = validLabel(label))
        db.roomDao().update(updated)
        return updated
    }

    // Deleting a room with evidence requires an explicit decision:
    // pass allowWithEvidence = true to confirm. Frozen when sealed.
    suspend fun deleteRoom(roomId: String, allowWithEvidence: Boolean = false) {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val attached = db.evidenceDao().listForRoom(roomId)
        if (attached.isNotEmpty() && !allowWithEvidence) {
            throw LocalValidationException("room has ${attached.size} evidence rows; confirm deletion")
        }
        if (attached.isNotEmpty()) {
            throw LocalValidationException("room has evidence; move or remove it first")
        }
        db.roomDao().delete(roomId)
    }

    suspend fun setRoomNotApplicable(roomId: String, na: Set<ShotCategory>): LocalRoom {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val updated = room.copy(naCategories = na.joinToString(",") { it.name })
        db.roomDao().update(updated)
        return updated
    }

    suspend fun recordEvidence(
        inspectionId: String,
        roomId: String?,
        fileName: String,
        mime: String,
        size: Long,
        sha256: String,
        origin: String = "capture",
        shotCategory: ShotCategory? = null,
        capturedAtDevice: String = deviceNowIso(),
    ): LocalEvidence {
        requireDraft(inspectionId)
        if (roomId != null) requireRoom(roomId)
        if (fileName.isBlank()) throw LocalValidationException("fileName required")
        if (size <= 0) throw LocalValidationException("size must be positive")
        return db.evidenceDao().insertSequenced(inspectionId) { sequence ->
            LocalEvidence(
                id = newLocalId("ev"), inspectionId = inspectionId, roomId = roomId,
                fileName = fileName, mime = mime, size = size, sha256 = sha256,
                sequence = sequence, capturedAtDevice = capturedAtDevice, origin = origin,
                shotCategory = shotCategory?.name, latitude = null, longitude = null,
                accuracyM = null, locationStatus = "unavailable",
            )
        }
    }

    suspend fun addFinding(
        roomId: String,
        evidenceId: String,
        preset: FindingPreset,
        note: String? = null,
    ): LocalFinding {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val evidence = db.evidenceDao().get(evidenceId)
            ?: throw LocalValidationException("evidence not found")
        if (evidence.inspectionId != room.inspectionId) {
            throw LocalValidationException("evidence belongs to another inspection")
        }
        val row = LocalFinding(
            id = newLocalId("find"), roomId = roomId, evidenceId = evidenceId,
            preset = preset.name, note = note?.takeIf { it.isNotBlank() },
        )
        db.findingDao().insert(row)
        return row
    }

    // Reads: resume navigation after process death.
    suspend fun getInspection(id: String): LocalInspection? = db.inspectionDao().get(id)

    suspend fun listDrafts(): List<LocalInspection> = db.inspectionDao().listDrafts()

    suspend fun roomsFor(inspectionId: String): List<LocalRoom> =
        db.roomDao().listForInspection(inspectionId)

    suspend fun evidencesFor(inspectionId: String): List<LocalEvidence> =
        db.evidenceDao().listForInspection(inspectionId)

    private suspend fun requireProperty(id: String) {
        if (db.propertyDao().get(id) == null) throw LocalValidationException("property not found")
    }

    private suspend fun requireTenancy(id: String) {
        if (db.tenancyDao().get(id) == null) throw LocalValidationException("tenancy not found")
    }

    private suspend fun requireRoom(id: String): LocalRoom =
        db.roomDao().get(id) ?: throw LocalValidationException("room not found")

    private suspend fun requireDraft(inspectionId: String): LocalInspection {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) {
            throw SealedModificationException("inspection ${inspection.id} is ${inspection.status}")
        }
        return inspection
    }

    private fun validLabel(label: String): String {
        val clean = label.trim()
        if (clean.isEmpty()) throw LocalValidationException("room label required")
        if (clean.length > MAX_ROOM_LABEL) throw LocalValidationException("label over $MAX_ROOM_LABEL chars")
        return clean
    }
}
