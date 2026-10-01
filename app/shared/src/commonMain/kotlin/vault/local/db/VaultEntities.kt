package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Local schema v1: capture core. Manifest/Report tables arrive with L5.
// Device time and GPS stay device claims; no server-certified columns here.
@Entity(tableName = "properties")
data class LocalProperty(
    @PrimaryKey val id: String,
    val address: String,
    // Structured parts (v12): checkout-style input. `address` stays the
    // formatted display line; legacy rows keep street backfilled from it.
    val street: String = "",
    val number: String = "",
    val complement: String? = null,
    val neighborhood: String = "",
    // Friendly name for the property picker ("Casa da praia").
    val nickname: String = "",
    // Local visibility for U07 removal: false = active, true = archived.
    // Organizational only, never part of frozen Manifest payloads.
    val archived: Boolean = false,
    val createdAtDevice: String,
)

@Entity(
    tableName = "tenancies",
    foreignKeys = [
        ForeignKey(
            entity = LocalProperty::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
        ),
    ],
    indices = [Index("propertyId")],
)
data class LocalTenancy(
    @PrimaryKey val id: String,
    val propertyId: String,
    val type: String,
    val createdAtDevice: String,
)

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = LocalTenancy::class,
            parentColumns = ["id"],
            childColumns = ["tenancyId"],
        ),
    ],
    indices = [Index("tenancyId")],
)
data class LocalInspection(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val kind: String,
    val status: String,
    val createdAtDevice: String,
    val sealedAtDevice: String?,
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
    ],
    indices = [Index("inspectionId")],
)
data class LocalRoom(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomKey: String,
    val label: String,
    val sort: Int,
    val naCategories: String,
    // Exit inspections mirror entry rooms: same labels/positions, new
    // ids, linked here for V1.3 side-by-side pairing.
    val sourceRoomId: String? = null,
)

@Entity(
    tableName = "evidences",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
        ForeignKey(
            entity = LocalRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
        ),
    ],
    indices = [Index("inspectionId"), Index("roomId")],
)
data class LocalEvidence(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomId: String?,
    val fileName: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val sequence: Int,
    val capturedAtDevice: String,
    val origin: String,
    val shotCategory: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val locationStatus: String,
    // v2: shutter-time claim details. EXIF stays a separate device claim.
    val fixAgeMs: Long? = null,
    val provider: String? = null,
    val accuracyLevel: String? = null,
    val isMocked: Boolean = false,
    val exifLatitude: Double? = null,
    val exifLongitude: Double? = null,
)

@Entity(
    tableName = "manifests",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
    ],
    indices = [Index(value = ["inspectionId"], unique = true)],
)
data class LocalManifest(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val formatVersion: Int,
    val canonicalSha256: String,
    val canonicalJson: String,
    val sealedAtDevice: String,
    val signerKeyId: String,
    val algorithm: String,
    val hardwareBacked: Boolean,
    val signature: String,
)

@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = LocalInspection::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
        ),
        ForeignKey(
            entity = LocalManifest::class,
            parentColumns = ["id"],
            childColumns = ["manifestId"],
        ),
    ],
    indices = [Index(value = ["manifestId"], unique = true)],
)
data class LocalReport(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val manifestId: String,
    val manifestSha256: String,
    // RENDERING | READY | FAILED. Retries update the same row;
    // SEALED never reopens.
    val status: String,
    val pdfSha256: String?,
    val pdfFileName: String?,
    val receiptSignature: String?,
    val signerKeyId: String,
    val algorithm: String,
    val hardwareBacked: Boolean,
    val createdAtDevice: String,
    val completedAtDevice: String?,
)

// Post-seal changes live here, never in the sealed inspection.
// Occurrences belong to the Property, so they survive across
// inspections and never reopen a terminal SEALED state.
@Entity(
    tableName = "occurrences",
    foreignKeys = [
        ForeignKey(
            entity = LocalProperty::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
        ),
    ],
    indices = [Index("propertyId")],
)
data class LocalOccurrence(
    @PrimaryKey val id: String,
    val propertyId: String,
    val title: String,
    val body: String?,
    val createdAtDevice: String,
)

@Entity(
    tableName = "occurrence_attachments",
    foreignKeys = [
        ForeignKey(
            entity = LocalOccurrence::class,
            parentColumns = ["id"],
            childColumns = ["occurrenceId"],
        ),
    ],
    indices = [Index("occurrenceId")],
)
data class OccurrenceAttachment(
    @PrimaryKey val id: String,
    val occurrenceId: String,
    val fileName: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val capturedAtDevice: String,
    val note: String?,
)

// Entry-vs-exit tags per room position. The pair references the latest
// photo of each side at tagging time; retakes don't rewrite history,
// they just become the new latest for the next tag.
@Entity(
    tableName = "comparisons",
    indices = [Index(value = ["exitRoomId", "shotCategory"], unique = true)],
)
data class Comparison(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val entryRoomId: String,
    val exitRoomId: String,
    val shotCategory: String,
    val entryEvidenceId: String?,
    val exitEvidenceId: String?,
    // no_change | changed | wear. Never liability, only visual state.
    val tag: String,
    val createdAtDevice: String,
)

// V1.5 visual-difference list. One row per exit position where an
// offline heuristic fired (presence, findings, user tag or location
// change). Byte difference alone never raises a row: two captures
// always differ at byte level. Review state only; never liability.
@Entity(
    tableName = "visual_diffs",
    indices = [
        Index("exitInspectionId"),
        Index(value = ["exitRoomId", "shotCategory"], unique = true),
    ],
)
data class VisualDiff(
    @PrimaryKey val id: String,
    val tenancyId: String,
    val exitInspectionId: String,
    val entryRoomId: String,
    val exitRoomId: String,
    val shotCategory: String,
    // "" when that side has no photo yet.
    val entryEvidenceId: String,
    val exitEvidenceId: String,
    // Comma-separated DiffSignal serials, strongest first.
    val signals: String,
    // PENDING | REVIEWED | DISMISSED.
    val status: String,
    val createdAtDevice: String,
)

// Trust roots the user explicitly accepted: pinned fingerprints or
// publisher-certified installers. Unknown by default; nothing here is
// ever provisioned silently by imported content.
// V1.4 inspector hints. Suggestions never decide: the user confirms
// (creates a Finding) or ignores. Sealed inspections freeze them.
@Entity(
    tableName = "ai_suggestions",
    indices = [
        Index("inspectionId"),
        Index(value = ["roomId", "evidenceId", "kind", "detail"], unique = true),
    ],
)
data class AiSuggestion(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val roomId: String,
    // "" when the hint is room-level (coverage gap).
    val evidenceId: String,
    // COVERAGE_GAP | POSSIBLE_STAIN | POSSIBLE_CRACK. Never liability.
    val kind: String,
    // Gap category name, "" for stain/crack hints.
    val detail: String,
    // PENDING | CONFIRMED | IGNORED.
    val status: String,
    val createdAtDevice: String,
)

@Entity(tableName = "trusted_issuers")
data class TrustedIssuer(
    @PrimaryKey val keyId: String,
    val label: String,
    // PINNED (compared fingerprint) or CERTIFIED (publisher certificate).
    val source: String,
    val addedAtDevice: String,
)

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = LocalRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
        ),
        ForeignKey(
            entity = LocalEvidence::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
        ),
    ],
    indices = [Index("roomId"), Index("evidenceId")],
)
data class LocalFinding(
    @PrimaryKey val id: String,
    val roomId: String,
    val evidenceId: String,
    val preset: String,
    val note: String?,
)

// User-customizable finding problems (v15). Built-ins seed lazily with
// stable enum-name codes; customs carry "custom:<id>" codes. Findings
// keep storing codes, so sealed bytes stay stable while labels evolve.
@Entity(tableName = "finding_problems")
data class LocalFindingProblem(
    @PrimaryKey val code: String,
    val label: String,
    val active: Boolean = true,
    val builtin: Boolean = false,
)
