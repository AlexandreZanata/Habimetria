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
        val row = LocalProperty(id = newLocalId("prop"), address = clean, createdAtDevice = deviceNowIso())
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

    suspend fun inspectionsForTenancy(tenancyId: String): List<LocalInspection> =
        db.inspectionDao().listForTenancy(tenancyId)

    suspend fun entryInspectionFor(tenancyId: String): LocalInspection? =
        db.inspectionDao().listForTenancy(tenancyId).firstOrNull { it.kind == "entry" }

    suspend fun exitInspectionFor(tenancyId: String): LocalInspection? =
        db.inspectionDao().listForTenancy(tenancyId).firstOrNull { it.kind == "exit" }

    // Exit inspection: allowed only after the entry is sealed, only once
    // per tenancy. Rooms mirror the entry (labels, order, N/A) with fresh
    // ids linked for V1.3 pairing; photos repeat per room/position.
    suspend fun startExitInspection(tenancyId: String): LocalInspection {
        requireTenancy(tenancyId)
        val entry = entryInspectionFor(tenancyId)
            ?: throw LocalValidationException("sealed entry required first")
        if (entry.status != STATUS_SEALED) {
            throw LocalValidationException("entry must be sealed first")
        }
        if (exitInspectionFor(tenancyId) != null) {
            throw LocalValidationException("exit already exists")
        }
        val exit = LocalInspection(newLocalId("ins"), tenancyId, "exit", STATUS_DRAFT, deviceNowIso(), null)
        db.inspectionDao().insert(exit)
        for (source in db.roomDao().listForInspection(entry.id)) {
            db.roomDao().insert(
                LocalRoom(
                    id = newLocalId("room"), inspectionId = exit.id,
                    roomKey = "room_" + newLocalId("k").substringAfter("_"),
                    label = source.label, sort = source.sort,
                    naCategories = source.naCategories, sourceRoomId = source.id,
                ),
            )
        }
        return exit
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

    // Reordering is DRAFT-only and renormalizes sort to 0..n-1.
    // Out-of-range targets clamp; sealed inspections refuse.
    suspend fun moveRoom(roomId: String, newIndex: Int): List<LocalRoom> {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val rooms = db.roomDao().listForInspection(room.inspectionId).toMutableList()
        rooms.removeAll { it.id == roomId }
        rooms.add(newIndex.coerceIn(0, rooms.size), room)
        rooms.forEachIndexed { index, current ->
            if (current.sort != index) db.roomDao().update(current.copy(sort = index))
        }
        return db.roomDao().listForInspection(room.inspectionId)
    }

    suspend fun setRoomNotApplicable(roomId: String, na: Set<ShotCategory>): LocalRoom {
        val room = requireRoom(roomId)
        requireDraft(room.inspectionId)
        val updated = room.copy(naCategories = na.joinToString(",") { it.name })
        db.roomDao().update(updated)
        return updated
    }

    // Production flow stores the original first (size+sha known), then
    // records the row with the same id so DB and files stay bound.
    // Location and EXIF ride along as device claims; a null claim keeps
    // the row explicitly without location.
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
        evidenceId: String = newLocalId("ev"),
        location: LocationClaim? = null,
        exif: ExifGps? = null,
    ): LocalEvidence {
        requireDraft(inspectionId)
        if (roomId != null) requireRoom(roomId)
        if (fileName.isBlank()) throw LocalValidationException("fileName required")
        if (size <= 0) throw LocalValidationException("size must be positive")
        location?.validate()
        return db.evidenceDao().insertSequenced(inspectionId) { sequence ->
            LocalEvidence(
                id = evidenceId, inspectionId = inspectionId, roomId = roomId,
                fileName = fileName, mime = mime, size = size, sha256 = sha256,
                sequence = sequence, capturedAtDevice = capturedAtDevice, origin = origin,
                shotCategory = shotCategory?.name,
                latitude = location?.latitude, longitude = location?.longitude,
                accuracyM = location?.accuracyM,
                locationStatus = location?.status?.name ?: LocationStatus.UNAVAILABLE.name,
                fixAgeMs = location?.fixAgeMs,
                provider = location?.provider,
                accuracyLevel = location?.accuracyLevel,
                isMocked = location?.isMocked == true,
                exifLatitude = exif?.latitude,
                exifLongitude = exif?.longitude,
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

    // Property address behind an inspection, for resume rows. Null when
    // any link in the chain is gone; never throws for UI listing.
    suspend fun addressFor(inspectionId: String): String? {
        val inspection = db.inspectionDao().get(inspectionId) ?: return null
        val tenancy = db.tenancyDao().get(inspection.tenancyId) ?: return null
        return db.propertyDao().get(tenancy.propertyId)?.address
    }

    suspend fun roomsFor(inspectionId: String): List<LocalRoom> =
        db.roomDao().listForInspection(inspectionId)

    suspend fun evidencesFor(inspectionId: String): List<LocalEvidence> =
        db.evidenceDao().listForInspection(inspectionId)

    suspend fun evidencesForRoom(roomId: String): List<LocalEvidence> =
        db.evidenceDao().listForRoom(roomId)

    suspend fun findingsForRoom(roomId: String): List<LocalFinding> =
        db.findingDao().listForRoom(roomId)

    suspend fun findingsForEvidence(evidenceId: String): List<LocalFinding> =
        db.findingDao().listForEvidence(evidenceId)

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
