package vault.local

import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase

interface PropertiesOperations {
    suspend fun createProperty(address: String): LocalProperty

    suspend fun createPropertyStructured(
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
        nickname: String = "",
    ): LocalProperty

    suspend fun updatePropertyDetails(
        propertyId: String,
        nickname: String,
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
    ): LocalProperty

    suspend fun listProperties(): List<LocalProperty>

    suspend fun getProperty(id: String): LocalProperty?

    suspend fun propertyOverviews(limit: Int = 100, offset: Int = 0): List<PropertyOverview>

    suspend fun inspectionsForProperty(propertyId: String, limit: Int = 50): List<LocalInspection>
}

internal class LocalProperties(private val db: VaultDatabase) : PropertiesOperations {
    private val guard = LocalWriteGuard(db)

    override suspend fun createProperty(address: String): LocalProperty = localWrite {
        val clean = address.trim()
        if (clean.length < 3) throw LocalValidationException("address too short")
        val row = LocalProperty(id = newLocalId("prop"), address = clean, createdAtDevice = deviceNowIso())
        db.propertyDao().insert(row)
        return@localWrite row
    }

    // Checkout-style address: street + number + optional complement +
    // neighborhood. The formatted line stays on `address` so every
    // consumer (review, PDF, resume) keeps working unchanged.

    override suspend fun createPropertyStructured(
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
        nickname: String,
    ): LocalProperty = localWrite {
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
        return@localWrite row
    }

    // Friendly name for the home picker. Organizational only: allowed
    // even when inspections under it are sealed.
    // Full data edit from the detail screen: same rules as creation.
    // Rebuilds the display line so every consumer keeps working unchanged.

    override suspend fun updatePropertyDetails(
        propertyId: String,
        nickname: String,
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
    ): LocalProperty = localWrite {
        val property = db.propertyDao().get(propertyId)
            ?: throw LocalValidationException("property not found")
        val cleanStreet = street.trim()
        val cleanNumber = number.trim()
        val cleanComplement = complement?.trim().orEmpty()
        val cleanNeighborhood = neighborhood.trim()
        if (cleanStreet.length < 3) throw LocalValidationException("street too short")
        if (cleanNumber.isEmpty()) throw LocalValidationException("number required")
        if (cleanNeighborhood.length < 2) throw LocalValidationException("neighborhood too short")
        val updated = property.copy(
            nickname = nickname.trim().take(MAX_NICKNAME),
            address = formatAddress(cleanStreet, cleanNumber, cleanComplement, cleanNeighborhood),
            street = cleanStreet,
            number = cleanNumber,
            complement = cleanComplement.ifEmpty { null },
            neighborhood = cleanNeighborhood,
        )
        db.propertyDao().update(updated)
        return@localWrite updated
    }

    override suspend fun listProperties(): List<LocalProperty> = db.propertyDao().listAll()

    override suspend fun getProperty(id: String): LocalProperty? = db.propertyDao().get(id)

    // Aggregated overview for the Imóveis list: one bounded page of
    // properties plus grouped inspection/room counts in 4 queries total.
    // UI lists must use this instead of per-card propertyFor/roomsFor
    // loops, which grow N+1 traffic on 100-property fixtures.

    override suspend fun propertyOverviews(limit: Int, offset: Int): List<PropertyOverview> {
        val boundedLimit = limit.coerceIn(1, 200)
        val safeOffset = offset.coerceAtLeast(0)
        val properties = db.propertyDao().listPaged(boundedLimit, safeOffset)
        if (properties.isEmpty()) return emptyList()
        val tenancies = db.tenancyDao().listForProperties(properties.map { it.id })
        val tenancyToProperty = tenancies.associate { it.id to it.propertyId }
        val inspections = db.inspectionDao().listForProperties(properties.map { it.id })
        val counts = if (inspections.isEmpty()) {
            emptyList()
        } else {
            db.roomDao().countsForInspections(inspections.map { it.id })
        }.associate { it.inspectionId to it.count }
        val byProperty = mutableMapOf<String, MutableList<LocalInspection>>()
        for (inspection in inspections) {
            val propertyId = inspection.propertyId ?: tenancyToProperty[inspection.tenancyId] ?: continue
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

    override suspend fun inspectionsForProperty(propertyId: String, limit: Int): List<LocalInspection> =
        db.inspectionDao().listForProperty(propertyId, limit.coerceIn(1, 200))

    // New Inspections require an active Property (U09): restore an
    // archived Property first. Resuming existing drafts never passes here.
}
