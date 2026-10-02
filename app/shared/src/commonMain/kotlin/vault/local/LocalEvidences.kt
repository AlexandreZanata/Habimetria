package vault.local

import vault.findings.FindingPreset
import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import vault.local.db.VaultDatabase

interface EvidenceOperations {
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
        deviceUtcOffsetMinutes: Int? = null,
        exifOrientation: Int? = null,
    ): LocalEvidence

    suspend fun deleteEvidence(evidenceId: String)

    suspend fun addFinding(
        roomId: String,
        evidenceId: String,
        preset: FindingPreset,
        note: String? = null,
    ): LocalFinding

    suspend fun addFindingWithCode(
        roomId: String,
        evidenceId: String,
        code: String,
        note: String? = null,
    ): LocalFinding

    suspend fun evidencesFor(inspectionId: String): List<LocalEvidence>

    suspend fun evidencesForRoom(roomId: String): List<LocalEvidence>

    suspend fun findingsForRoom(roomId: String): List<LocalFinding>

    suspend fun findingsForEvidence(evidenceId: String): List<LocalFinding>
}

internal class LocalEvidences(private val db: VaultDatabase) : EvidenceOperations {
    private val guard = LocalWriteGuard(db)

    override suspend fun recordEvidence(
        inspectionId: String,
        roomId: String?,
        fileName: String,
        mime: String,
        size: Long,
        sha256: String,
        origin: String,
        shotCategory: ShotCategory?,
        capturedAtDevice: String,
        evidenceId: String,
        location: LocationClaim?,
        exif: ExifGps?,
        deviceUtcOffsetMinutes: Int?,
        exifOrientation: Int?,
    ): LocalEvidence = localWrite {
        guard.requireDraft(inspectionId)
        if (roomId != null && guard.requireRoom(roomId).inspectionId != inspectionId) {
            throw LocalValidationException("room belongs to another inspection")
        }
        if (fileName.isBlank()) throw LocalValidationException("fileName required")
        if (size <= 0) throw LocalValidationException("size must be positive")
        location?.validate()
        require(deviceUtcOffsetMinutes == null || deviceUtcOffsetMinutes in -840..840)
        require(exifOrientation == null || exifOrientation in 1..8)
        return@localWrite db.evidenceDao().insertSequenced(inspectionId) { sequence ->
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
                mockSignal = location?.mockSignal, fixTimeDevice = location?.fixTimeDevice,
                deviceUtcOffsetMinutes = deviceUtcOffsetMinutes, exifOrientation = exifOrientation,
            )
        }
    }

    // Deleting one photo removes its findings first (FK has no cascade),
    // then the row. Files go through LocalSession so DB and disk stay
    // bound. Frozen when sealed, like every other edit.

    override suspend fun deleteEvidence(evidenceId: String) = localWrite {
        val evidence = db.evidenceDao().get(evidenceId)
            ?: throw LocalValidationException("evidence not found")
        guard.requireDraft(evidence.inspectionId)
        db.findingDao().deleteForEvidence(evidenceId)
        db.evidenceDao().delete(evidenceId)
    }

    override suspend fun addFinding(
        roomId: String,
        evidenceId: String,
        preset: FindingPreset,
        note: String?,
    ): LocalFinding = insertFinding(roomId, evidenceId, preset.name, note)

    // Code-based finding for customized problems: stores the stable code
    // (enum name or custom id). Unknown or inactive codes are rejected.

    override suspend fun addFindingWithCode(
        roomId: String,
        evidenceId: String,
        code: String,
        note: String?,
    ): LocalFinding {
        val option = FindingProblemRepository(db).resolveCode(code)
            ?: throw LocalValidationException("unknown problem")
        if (!option.active) throw LocalValidationException("problem not active")
        return insertFinding(roomId, evidenceId, option.code, note)
    }

    private suspend fun insertFinding(
        roomId: String,
        evidenceId: String,
        presetCode: String,
        note: String?,
    ): LocalFinding = localWrite {
        val room = guard.requireRoom(roomId)
        guard.requireDraft(room.inspectionId)
        val evidence = db.evidenceDao().get(evidenceId)
            ?: throw LocalValidationException("evidence not found")
        if (evidence.inspectionId != room.inspectionId || evidence.roomId != roomId) {
            throw LocalValidationException("evidence belongs to another inspection")
        }
        val row = LocalFinding(
            id = newLocalId("find"), roomId = roomId, evidenceId = evidenceId,
            preset = presetCode, note = note?.takeIf { it.isNotBlank() },
        )
        db.findingDao().insert(row)
        return@localWrite row
    }

    // Reads: resume navigation after process death.

    override suspend fun evidencesFor(inspectionId: String): List<LocalEvidence> =
        db.evidenceDao().listForInspection(inspectionId)

    override suspend fun evidencesForRoom(roomId: String): List<LocalEvidence> =
        db.evidenceDao().listForRoom(roomId)

    override suspend fun findingsForRoom(roomId: String): List<LocalFinding> =
        db.findingDao().listForRoom(roomId)

    override suspend fun findingsForEvidence(evidenceId: String): List<LocalFinding> =
        db.findingDao().listForEvidence(evidenceId)
}
