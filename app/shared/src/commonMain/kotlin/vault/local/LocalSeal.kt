package vault.local

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.LocalManifest
import vault.local.db.VaultDatabase

class SealBlockedException(val reasons: List<String>) :
    IllegalStateException("seal blocked: ${reasons.joinToString(", ")}")

// Eligibility reason codes (UI maps them to pt-BR copy).
const val SEAL_EMPTY = "empty"
const val SEAL_UNREADABLE = "unreadable"
const val SEAL_ALREADY = "already-sealed"
const val SEAL_NOT_DRAFT = "not-draft"
const val SEAL_REPORT_DETAILS = "report-details"

data class CoverageGap(
    val roomId: String,
    val label: String,
    val percent: Int,
    val missing: List<ShotCategory>,
)

data class SealEligibility(
    val inspectionId: String,
    val rooms: Int,
    val evidences: Int,
    val findings: Int,
    val gaps: List<CoverageGap>,
    val unreadable: List<String>,
    val alreadySealed: Boolean,
    val canSeal: Boolean,
    val reasons: List<String>,
)

sealed interface SealOutcome {
    data class Sealed(val manifest: LocalManifest) : SealOutcome
    data class AlreadySealed(val manifest: LocalManifest) : SealOutcome
}

// LocalSealer freezes the immutable snapshot: validate originals, bind
// every hash and metadata byte into canonical form, sign, then commit
// manifest + SEALED atomically. Retries return the same snapshot
// (identity = manifest hash); concurrent seals collapse to one row.
class LocalSealer(
    private val db: VaultDatabase,
    private val originals: OriginalStore,
    private val signer: InstallationSigner,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    private val coverage = VaultCoverage(db)

    suspend fun assess(inspectionId: String): SealEligibility {
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        val existing = db.manifestDao().getByInspection(inspectionId)
        val rooms = db.roomDao().listForInspection(inspectionId)
        val evidences = db.evidenceDao().listForInspection(inspectionId)
        val findings = rooms.flatMap { db.findingDao().listForRoom(it.id) }
        val gaps = rooms.map { room ->
            val roomCoverage = coverage.roomCoverage(room.id)
            CoverageGap(room.id, room.label, roomCoverage.percent(), coverage.missingCategories(room.id))
        }
        val unreadable = evidences.filterNot { evidence ->
            val path = originals.originalPath(evidence.id).toPath()
            try {
                fs.exists(path) && (fs.metadataOrNull(path)?.size == evidence.size)
            } catch (_: Exception) {
                false
            }
        }.map { it.id }
        val reasons = buildList {
            if (existing != null || inspection.status != STATUS_DRAFT) {
                add(if (existing != null) SEAL_ALREADY else SEAL_NOT_DRAFT)
            }
            if (evidences.isEmpty()) add(SEAL_EMPTY)
            if (inspection.kind == "technical" && runCatching { ReportDetailsRepository(db).get(inspectionId).validate(inspection.kind) }.isFailure) add(SEAL_REPORT_DETAILS)
            if (unreadable.isNotEmpty()) add("$SEAL_UNREADABLE:${unreadable.size}")
        }
        return SealEligibility(
            inspectionId = inspectionId,
            rooms = rooms.size,
            evidences = evidences.size,
            findings = findings.size,
            gaps = gaps,
            unreadable = unreadable,
            alreadySealed = existing != null,
            canSeal = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    suspend fun seal(inspectionId: String): SealOutcome = localWrite {
        db.manifestDao().getByInspection(inspectionId)?.let { return@localWrite SealOutcome.AlreadySealed(it) }
        val inspection = db.inspectionDao().get(inspectionId)
            ?: throw LocalValidationException("inspection not found")
        if (inspection.status != STATUS_DRAFT) throw SealBlockedException(listOf(SEAL_NOT_DRAFT))
        val eligibility = assess(inspectionId)
        if (!eligibility.canSeal) throw SealBlockedException(eligibility.reasons)

        val tenancy = inspection.tenancyId?.let { db.tenancyDao().get(it) }
        val propertyId = inspection.propertyId ?: tenancy?.propertyId ?: throw LocalValidationException("property link missing")
        val property = db.propertyDao().get(propertyId)
            ?: throw LocalValidationException("property not found")
        val sealedAt = deviceNowIso()
        val canonical = canonicalManifestJson(
            ManifestInput(
                propertyId = property.id,
                address = property.address,
                tenancy = tenancy,
                inspection = inspection,
                rooms = db.roomDao().listForInspection(inspectionId),
                evidences = db.evidenceDao().listForInspection(inspectionId),
                findings = db.roomDao().listForInspection(inspectionId)
                    .flatMap { db.findingDao().listForRoom(it.id) },
                sealedAtDevice = sealedAt,
                details = ReportDetailsRepository(db).get(inspectionId).validate(inspection.kind),
                findingLabels = db.roomDao().listForInspection(inspectionId).flatMap { db.findingDao().listForRoom(it.id) }
                    .map { it.preset }.distinct().associateWith { code ->
                        FindingProblemRepository(db).resolveCode(code)?.label ?: code
                    },
            ),
        )
        for (evidence in db.evidenceDao().listForInspection(inspectionId)) {
            ReportSources(originals, fs).original(evidence)
        }
        SignerMaterial(originals.filesDir, fs).retain(signer)
        val hash = manifestSha256(canonical)
        val signature = signer.sign(canonical.encodeToByteArray()).toByteString().hex()
        val manifest = LocalManifest(
            id = newLocalId("man"),
            inspectionId = inspectionId,
            formatVersion = MANIFEST_FORMAT_VERSION,
            canonicalSha256 = hash,
            canonicalJson = canonical,
            sealedAtDevice = sealedAt,
            signerKeyId = signer.keyId,
            algorithm = SEAL_ALGORITHM,
            hardwareBacked = signer.hardwareBacked,
            signature = signature,
        )
        try {
            val marked = db.manifestDao().commitSeal(manifest, inspectionId, sealedAt)
            if (marked == 0) {
                // Lost race with a concurrent seal: return the winner's row.
                db.manifestDao().getByInspection(inspectionId)?.let { return@localWrite SealOutcome.AlreadySealed(it) }
            }
        } catch (e: Exception) {
            // Insert aborted on the unique binding: same outcome.
            db.manifestDao().getByInspection(inspectionId)?.let { return@localWrite SealOutcome.AlreadySealed(it) }
            throw e
        }
        return@localWrite SealOutcome.Sealed(manifest)
    }

    // Any alteration — canonical byte, Room label, note, GPS value —
    // fails verification. Malformed signatures fail closed (false).
    fun verifyManifest(manifest: LocalManifest, publicKey: ByteArray): Boolean =
        try {
            manifest.algorithm == SEAL_ALGORITHM &&
            keyFingerprint(publicKey) == manifest.signerKeyId &&
            manifestSha256(manifest.canonicalJson) == manifest.canonicalSha256 &&
            signer.verify(
                publicKey,
                manifest.canonicalJson.encodeToByteArray(),
                manifest.signature.decodeHex().toByteArray(),
            )
        } catch (_: Exception) {
            false
        }
}
