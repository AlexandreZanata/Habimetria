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

