package vault.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import vault.api.AppContext

// Foreground-only reads of the cached fix via LocationManager (no
// Play-services dependency, fully offline). Capture calls lastClaim and
// moves on; a missing fix is UNAVAILABLE, never synthesized.
actual fun platformLocationTracker(): LocationTracker = AndroidLocationTracker(AppContext.app)

class AndroidLocationTracker(private val context: Context) : LocationTracker {
    override suspend fun lastClaim(): LocationClaim {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) return denied()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fixes = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider ->
                try {
                    manager.isProviderEnabled(provider)
                } catch (_: Exception) {
                    false
                }
            }
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
        if (fixes.isEmpty()) return LocationClaim(null, null, null, null, null, null, LocationStatus.UNAVAILABLE)
        val fix = fixes.maxByOrNull { it.time }!!
        val ageMs = (android.os.SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000
        val mocked = if (Build.VERSION.SDK_INT >= 31) {
            fix.isMock
        } else {
            @Suppress("DEPRECATION")
            fix.isFromMockProvider
        }
        val status = if (!fine || (fix.hasAccuracy() && fix.accuracy > 100f)) {
            LocationStatus.APPROXIMATE
        } else {
            LocationStatus.PRECISE
        }
        return LocationClaim(
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyM = fix.accuracy.takeIf { fix.hasAccuracy() },
            fixAgeMs = ageMs.coerceAtLeast(0),
            provider = fix.provider,
            accuracyLevel = if (fine) "fine" else "coarse",
            status = status,
            isMocked = mocked,
            mockSignal = if (mocked) "MOCKED" else "NOT_INDICATED",
            fixTimeDevice = java.time.Instant.ofEpochMilli(fix.time).toString(),
        ).validate()
    }

    private fun granted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun denied(): LocationClaim =
        LocationClaim(null, null, null, null, null, null, LocationStatus.DENIED)
}
