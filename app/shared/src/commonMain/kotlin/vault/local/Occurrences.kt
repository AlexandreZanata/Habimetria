package vault.local

import vault.local.db.LocalOccurrence
import vault.local.db.OccurrenceAttachment
import vault.local.db.VaultDatabase

const val MAX_OCCURRENCE_TITLE = 120

// Post-seal story: occurrences attach to the Property and never mutate
// any Inspection, sealed or not. Photos follow the same store-first
// discipline as evidences (size+sha known before the row exists).
class LocalOccurrenceRepository(private val db: VaultDatabase) {

    suspend fun createOccurrence(propertyId: String, title: String, body: String?): LocalOccurrence {
        if (db.propertyDao().get(propertyId) == null) {
            throw LocalValidationException("property not found")
        }
        val clean = title.trim()
        if (clean.isEmpty()) throw LocalValidationException("occurrence title required")
        if (clean.length > MAX_OCCURRENCE_TITLE) {
            throw LocalValidationException("title over $MAX_OCCURRENCE_TITLE chars")
        }
        val row = LocalOccurrence(
            id = newLocalId("occ"), propertyId = propertyId, title = clean,
            body = body?.takeIf { it.isNotBlank() }, createdAtDevice = deviceNowIso(),
        )
        db.occurrenceDao().insert(row)
        return row
    }

    suspend fun attachPhoto(
        occurrenceId: String,
        fileName: String,
        mime: String,
        size: Long,
        sha256: String,
        note: String? = null,
        attachmentId: String = newLocalId("att"),
    ): OccurrenceAttachment {
        if (db.occurrenceDao().get(occurrenceId) == null) {
            throw LocalValidationException("occurrence not found")
        }
        if (fileName.isBlank()) throw LocalValidationException("fileName required")
        if (size <= 0) throw LocalValidationException("size must be positive")
        val row = OccurrenceAttachment(
            id = attachmentId, occurrenceId = occurrenceId, fileName = fileName,
            mime = mime, size = size, sha256 = sha256,
            capturedAtDevice = deviceNowIso(), note = note?.takeIf { it.isNotBlank() },
        )
        db.occurrenceAttachmentDao().insert(row)
        return row
    }

    suspend fun listForProperty(propertyId: String): List<LocalOccurrence> =
        db.occurrenceDao().listForProperty(propertyId)

    suspend fun attachmentsFor(occurrenceId: String): List<OccurrenceAttachment> =
        db.occurrenceAttachmentDao().listForOccurrence(occurrenceId)

    // UI bridge: from any inspection (sealed or not) to its property's
    // occurrence timeline without touching inspection state.
    suspend fun propertyForInspection(inspectionId: String): String {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        inspection.propertyId?.let { return it }
        val tenancy = inspection.tenancyId?.let { db.tenancyDao().get(it) }
            ?: throw LocalValidationException("tenancy not found")
        return tenancy.propertyId
    }
}
