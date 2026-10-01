package vault.ui

import vault.local.LocationStatus
import vault.local.STALE_FIX_AGE_MS

// One honest line for any shutter-time claim. Stale and simulated fixes
// stay labeled; missing coordinates never borrow another photo's values.
fun claimSummary(
    status: String?,
    accuracyM: Float?,
    fixAgeMs: Long?,
    isMocked: Boolean,
): String {
    val base = when (status) {
        LocationStatus.PRECISE.name -> {
            val tail = if (accuracyM != null) " (${accuracyM.toInt()} m)" else ""
            Strings.locationPrecise + tail
        }
        LocationStatus.APPROXIMATE.name -> {
            val tail = if (accuracyM != null) " (~${accuracyM.toInt()} m)" else ""
            Strings.locationApproximate + tail
        }
        LocationStatus.DENIED.name -> Strings.locationDenied
        else -> Strings.locationUnavailable
    }
    val stale = fixAgeMs != null && fixAgeMs > STALE_FIX_AGE_MS
    val flags = buildList {
        if (stale) add(Strings.locationStale)
        if (isMocked) add(Strings.locationMocked)
    }
    return if (flags.isEmpty()) base else "$base (${flags.joinToString(", ")})"
}
