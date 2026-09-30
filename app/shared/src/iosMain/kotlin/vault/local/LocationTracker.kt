package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSinceNow

// Foreground Core Location behind the shared interface. The manager keeps
// a cached fix warm; lastClaim reads it synchronously and never blocks
// the shutter. iOS exposes no public mock-location flag, so isMocked
// stays false here (documented limitation, not a fabricated false).
@OptIn(ExperimentalForeignApi::class)
actual fun platformLocationTracker(): LocationTracker = IosLocationTracker()

@OptIn(ExperimentalForeignApi::class)
class IosLocationTracker : LocationTracker {
    private val manager = CLLocationManager().also {
        it.desiredAccuracy = kCLLocationAccuracyBest
    }

    override suspend fun lastClaim(): LocationClaim {
        manager.requestWhenInUseAuthorization()
        val fix = manager.location
        if (fix == null) {
            return LocationClaim(null, null, null, null, null, null, LocationStatus.UNAVAILABLE)
        }
        val ageMs = (-fix.timestamp.timeIntervalSinceNow() * 1000).toLong().coerceAtLeast(0)
        val precise =
            manager.accuracyAuthorization == CLAccuracyAuthorizationFullAccuracy
        return LocationClaim(
            latitude = fix.coordinate.useContents { latitude },
            longitude = fix.coordinate.useContents { longitude },
            accuracyM = fix.horizontalAccuracy.toFloat().takeIf { it >= 0 },
            fixAgeMs = ageMs,
            provider = "core-location",
            accuracyLevel = if (precise) "full" else "reduced",
            status = if (precise) LocationStatus.PRECISE else LocationStatus.APPROXIMATE,
            isMocked = false,
        ).validate()
    }
}
