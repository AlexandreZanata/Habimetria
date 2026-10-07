package vault.local

import vault.local.db.LocalEvidence

// Short on-photo location stamp drawn over the photo rect (bottom-right)
// in the Report PDF, mirroring field geotag-camera stamps: device capture
// time, reported place, and device coordinates. Presentation of frozen
// snapshot data only: originals and derivatives never change and no new
// sealed field is introduced. Missing GPS stays explicit; the reported
// address is a user-supplied place, never a GPS-certified fact.
internal fun photoStampLines(evidence: LocalEvidence, address: String): List<String> = buildList {
    add("${evidence.capturedAtDevice} (hora do dispositivo)")
    val place = address.trim()
    if (place.isNotEmpty()) add(place)
    if (evidence.latitude == null || evidence.longitude == null) {
        add("GPS indisponível: " + when (evidence.locationStatus) {
            "DENIED" -> "permissão negada"
            "DISABLED" -> "localização desativada"
            else -> "sem leitura registrada"
        })
    } else {
        val accuracy = evidence.accuracyM?.let { " · ±${canonicalFloat(it)} m" } ?: ""
        val approximate = if (evidence.locationStatus == "APPROXIMATE") " (aproximada)" else ""
        add("${canonicalDouble(evidence.latitude)}, ${canonicalDouble(evidence.longitude)}$accuracy$approximate")
    }
}

// Wraps stamp lines to the photo width so the overlay never spills past
// the rect. Every character is preserved; wrapping only adds line breaks.
internal fun wrapStampLines(
    lines: List<String>,
    maxWidth: Double,
    measure: (String, TextKind, Double) -> List<String>,
): List<String> = lines.filter { it.isNotBlank() }
    .flatMap { measure(it, TextKind.SMALL, maxWidth) }
    .filter { it.isNotEmpty() }
