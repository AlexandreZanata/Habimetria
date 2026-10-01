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
const val MAX_NICKNAME = 40
const val STATUS_DRAFT = "DRAFT"
const val STATUS_SEALED = "SEALED"

// Structured address parts for prefill and display.
data class AddressParts(
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
)

// Brazilian display line: "Rua das Flores, 42, Apto 101 — Centro".
fun formatAddress(street: String, number: String, complement: String, neighborhood: String): String {
    val head = "$street, $number" + (complement.ifEmpty { null }?.let { ", $it" } ?: "")
    return "$head — $neighborhood"
}

// Aggregated row for the Imóveis list. Counts come from grouped queries;
// latestDraftId drives Continuar without extra lookups.
data class PropertyOverview(
    val property: LocalProperty,
    val draftCount: Int,
    val sealedCount: Int,
    val totalRooms: Int,
    val latestDraftId: String?,
    val latestStatus: String?,
)

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

    // Checkout-style address: street + number + optional complement +
    // neighborhood. The formatted line stays on `address` so every
    // consumer (review, PDF, resume) keeps working unchanged.
    suspend fun createPropertyStructured(
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
        nickname: String = "",
    ): LocalProperty {
        val cleanStreet = street.trim()
        val cleanNumber = number.trim()
        val cleanComplement = complement?.trim().orEmpty()
        val cleanNeighborhood = neighborhood.trim()
        if (cleanStreet.length < 3) throw LocalValidationException("street too short")
        if (cleanNumber.isEmpty()) throw LocalValidationException("number required")
        if (cleanNeighborhood.length < 2) throw LocalValidationException("neighborhood too short")
        val row = LocalProperty(
            id = newLocalId("prop"),
            address = formatAddress(cleanStreet, cleanNumber, cleanComplement, cleanNeighborhood),
            street = cleanStreet,
            number = cleanNumber,
            complement = cleanComplement.ifEmpty { null },
            neighborhood = cleanNeighborhood,
            nickname = nickname.trim().take(MAX_NICKNAME),
            createdAtDevice = deviceNowIso(),
        )
        db.propertyDao().insert(row)
        return row
    }

    // Friendly name for the home picker. Organizational only: allowed
    // even when inspections under it are sealed.
    suspend fun setNickname(propertyId: String, nickname: String): LocalProperty {
        val property = db.propertyDao().get(propertyId)
            ?: throw LocalValidationException("property not found")
        val updated = property.copy(nickname = nickname.trim().take(MAX_NICKNAME))
        db.propertyDao().update(updated)
        return updated
    }

    suspend fun listProperties(): List<LocalProperty> = db.propertyDao().listAll()

    suspend fun getProperty(id: String): LocalProperty? = db.propertyDao().get(id)

    // Aggregated overview for the Imóveis list: one bounded page of
    // properties plus grouped inspection/room counts in 4 queries total.
    // UI lists must use this instead of per-card propertyFor/roomsFor
    // loops, which grow N+1 traffic on 100-property fixtures.
    suspend fun propertyOverviews(limit: Int = 100, offset: Int = 0): List<PropertyOverview> {
        val boundedLimit = limit.coerceIn(1, 200)
        val safeOffset = offset.coerceAtLeast(0)
        val properties = db.propertyDao().listPaged(boundedLimit, safeOffset)
        if (properties.isEmpty()) return emptyList()
        val tenancies = db.tenancyDao().listForProperties(properties.map { it.id })
        val tenancyToProperty = tenancies.associate { it.id to it.propertyId }
        val inspections = if (tenancies.isEmpty()) {
            emptyList()
        } else {
            db.inspectionDao().listForTenancies(tenancies.map { it.id })
        }
        val counts = if (inspections.isEmpty()) {
            emptyList()
        } else {
            db.roomDao().countsForInspections(inspections.map { it.id })
        }.associate { it.inspectionId to it.count }
        val byProperty = mutableMapOf<String, MutableList<LocalInspection>>()
        for (inspection in inspections) {
            val propertyId = tenancyToProperty[inspection.tenancyId] ?: continue
            byProperty.getOrPut(propertyId) { mutableListOf() }.add(inspection)
        }
        return properties.map { property ->
            val related = byProperty[property.id].orEmpty().sortedByDescending { it.createdAtDevice }
            val drafts = related.filter { it.status == STATUS_DRAFT }
            PropertyOverview(
                property = property,
                draftCount = drafts.size,
                sealedCount = related.size - drafts.size,
                totalRooms = related.sumOf { counts[it.id] ?: 0 },
                latestDraftId = drafts.firstOrNull()?.id,
                latestStatus = related.firstOrNull()?.status,
            )
        }
    }

    // Bounded history for the property detail: DRAFT resumes capture,
    // SEALED opens read-only review. No per-row follow-up queries.
    suspend fun inspectionsForProperty(propertyId: String, limit: Int = 50): List<LocalInspection> =
        db.inspectionDao().listForProperty(propertyId, limit.coerceIn(1, 200))

    // New Inspections require an active Property (U09): restore an
    // archived Property first. Resuming existing drafts never passes here.
    suspend fun createTenancy(propertyId: String, type: String = "entry"): LocalTenancy {
        val property = db.propertyDao().get(propertyId)
            ?: throw LocalValidationException("property not found")
        if (property.archived) throw LocalValidationException("property archived")
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

    // Deleting one photo removes its findings first (FK has no cascade),
    // then the row. Files go through LocalSession so DB and disk stay
    // bound. Frozen when sealed, like every other edit.
    suspend fun deleteEvidence(evidenceId: String) {
        val evidence = db.evidenceDao().get(evidenceId)
            ?: throw LocalValidationException("evidence not found")
        requireDraft(evidence.inspectionId)
        db.findingDao().deleteForEvidence(evidenceId)
        db.evidenceDao().delete(evidenceId)
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

    // Property behind an inspection, for grouping drafts on Home.
    // Null when the chain is gone; never throws for UI listing.
    suspend fun propertyFor(inspectionId: String): LocalProperty? {
        val inspection = db.inspectionDao().get(inspectionId) ?: return null
        val tenancy = db.tenancyDao().get(inspection.tenancyId) ?: return null
        return db.propertyDao().get(tenancy.propertyId)
    }

    // Structured parts behind an inspection, for prefilling the Home
    // form when the user goes back. Null when the chain is gone.
    suspend fun addressPartsFor(inspectionId: String): AddressParts? {
        val inspection = db.inspectionDao().get(inspectionId) ?: return null
        val tenancy = db.tenancyDao().get(inspection.tenancyId) ?: return null
        val property = db.propertyDao().get(tenancy.propertyId) ?: return null
        return AddressParts(
            street = property.street.ifEmpty { property.address },
            number = property.number,
            complement = property.complement.orEmpty(),
            neighborhood = property.neighborhood,
        )
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
