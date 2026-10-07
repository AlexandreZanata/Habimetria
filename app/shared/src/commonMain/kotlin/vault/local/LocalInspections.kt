package vault.local

import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase

interface InspectionsOperations {
    suspend fun createTechnicalInspection(propertyId: String): LocalInspection

    suspend fun createTenancy(propertyId: String, type: String = "entry"): LocalTenancy

    suspend fun createInspection(tenancyId: String, kind: String = "entry"): LocalInspection

    suspend fun inspectionsForTenancy(tenancyId: String): List<LocalInspection>

    suspend fun entryInspectionFor(tenancyId: String): LocalInspection?

    suspend fun exitInspectionFor(tenancyId: String): LocalInspection?

    suspend fun startExitInspection(tenancyId: String): LocalInspection

    suspend fun getInspection(id: String): LocalInspection?

    suspend fun listDrafts(): List<LocalInspection>

    suspend fun addressFor(inspectionId: String): String?

    suspend fun propertyFor(inspectionId: String): LocalProperty?

    suspend fun addressPartsFor(inspectionId: String): AddressParts?
}

internal class LocalInspections(private val db: VaultDatabase) : InspectionsOperations {
    private val guard = LocalWriteGuard(db)

    override suspend fun createTechnicalInspection(propertyId: String): LocalInspection = localWrite {
        val property = db.propertyDao().get(propertyId) ?: throw LocalValidationException("property not found")
        require(!property.archived) { "property archived" }
        val row = LocalInspection(newLocalId("ins"), null, "technical", STATUS_DRAFT, deviceNowIso(), null, property.id)
        db.inspectionDao().insert(row)
        return@localWrite row
    }

    override suspend fun createTenancy(propertyId: String, type: String): LocalTenancy = localWrite {
        val property = db.propertyDao().get(propertyId)
            ?: throw LocalValidationException("property not found")
        if (property.archived) throw LocalValidationException("property archived")
        val row = LocalTenancy(newLocalId("ten"), propertyId, type, deviceNowIso())
        db.tenancyDao().insert(row)
        return@localWrite row
    }

    override suspend fun createInspection(tenancyId: String, kind: String): LocalInspection = localWrite {
        guard.requireTenancy(tenancyId)
        require(kind in listOf("entry", "exit")) { "unsupported tenancy inspection kind" }
        val propertyId = db.tenancyDao().get(tenancyId)!!.propertyId
        val row = LocalInspection(newLocalId("ins"), tenancyId, kind, STATUS_DRAFT, deviceNowIso(), null, propertyId)
        db.inspectionDao().insert(row)
        return@localWrite row
    }

    override suspend fun inspectionsForTenancy(tenancyId: String): List<LocalInspection> =
        db.inspectionDao().listForTenancy(tenancyId)

    override suspend fun entryInspectionFor(tenancyId: String): LocalInspection? =
        db.inspectionDao().listForTenancy(tenancyId).firstOrNull { it.kind == "entry" }

    override suspend fun exitInspectionFor(tenancyId: String): LocalInspection? =
        db.inspectionDao().listForTenancy(tenancyId).firstOrNull { it.kind == "exit" }

    // Exit inspection: allowed only after the entry is sealed, only once
    // per tenancy. Rooms mirror the entry (labels, order, N/A) with fresh
    // ids linked for V1.3 pairing; photos repeat per room/position.

    override suspend fun startExitInspection(tenancyId: String): LocalInspection = localWrite {
        guard.requireTenancy(tenancyId)
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
        return@localWrite exit
    }

    override suspend fun getInspection(id: String): LocalInspection? = db.inspectionDao().get(id)

    override suspend fun listDrafts(): List<LocalInspection> = db.inspectionDao().listDrafts()

    // Property address behind an inspection, for resume rows. Null when
    // any link in the chain is gone; never throws for UI listing.

    override suspend fun addressFor(inspectionId: String): String? {
        val inspection = db.inspectionDao().get(inspectionId) ?: return null
        return propertyFor(inspectionId)?.address
    }

    // Property behind an inspection, for grouping drafts on Home.
    // Null when the chain is gone; never throws for UI listing.

    override suspend fun propertyFor(inspectionId: String): LocalProperty? {
        val inspection = db.inspectionDao().get(inspectionId) ?: return null
        val propertyId = inspection.propertyId ?: inspection.tenancyId?.let { db.tenancyDao().get(it)?.propertyId } ?: return null
        return db.propertyDao().get(propertyId)
    }

    // Structured parts behind an inspection, for prefilling the Home
    // form when the user goes back. Null when the chain is gone.

    override suspend fun addressPartsFor(inspectionId: String): AddressParts? {
        val property = propertyFor(inspectionId) ?: return null
        return AddressParts(
            street = property.street.ifEmpty { property.address },
            number = property.number,
            complement = property.complement.orEmpty(),
            neighborhood = property.neighborhood,
        )
    }
}
