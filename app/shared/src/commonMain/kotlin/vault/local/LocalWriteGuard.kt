package vault.local

import vault.local.db.*

internal class LocalWriteGuard(private val db: VaultDatabase) {
    suspend fun requireTenancy(id: String) {
        if (db.tenancyDao().get(id) == null) throw LocalValidationException("tenancy not found")
    }

    suspend fun requireRoom(id: String): LocalRoom =
        db.roomDao().get(id) ?: throw LocalValidationException("room not found")

    suspend fun requireDraft(inspectionId: String): LocalInspection {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) {
            throw SealedModificationException("inspection ${inspection.id} is ${inspection.status}")
        }
        return inspection
    }

    fun validLabel(label: String): String {
        val clean = label.trim()
        if (clean.isEmpty()) throw LocalValidationException("room label required")
        if (clean.length > MAX_ROOM_LABEL) throw LocalValidationException("label over $MAX_ROOM_LABEL chars")
        return clean
    }
}
