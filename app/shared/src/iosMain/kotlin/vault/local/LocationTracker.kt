package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.timeIntervalSinceNow
import platform.Foundation.NSError
import platform.darwin.NSObject

// Foreground Core Location behind the shared interface. The manager keeps
// a cached fix warm; lastClaim reads it synchronously and never blocks
// the shutter. Source information is optional; absent stays unknown.
@OptIn(ExperimentalForeignApi::class)
actual fun platformLocationTracker(): LocationTracker = IosLocationTracker()

@OptIn(ExperimentalForeignApi::class)
class IosLocationTracker : LocationTracker {
    private var pending = false
    private val manager = CLLocationManager().also {
        it.desiredAccuracy = kCLLocationAccuracyBest
    }
    // Core Location retains its delegate weakly. Keep it for the tracker lifetime.
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            refresh()
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            pending = false
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            pending = false
        }
    }

    init {
        manager.delegate = delegate
        refresh()
    }

    private fun refresh() {
        val authorized = manager.authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways
        if (authorized && CLLocationManager.locationServicesEnabled() && !pending) {
            pending = true
            manager.requestLocation()
        }
    }

    override suspend fun lastClaim(): LocationClaim = withContext(Dispatchers.Main) {
        manager.requestWhenInUseAuthorization()
        if (manager.authorizationStatus == kCLAuthorizationStatusDenied || manager.authorizationStatus == kCLAuthorizationStatusRestricted)
            return@withContext LocationClaim(null, null, null, null, null, null, LocationStatus.DENIED)
        if (!CLLocationManager.locationServicesEnabled())
            return@withContext LocationClaim(null, null, null, null, null, null, LocationStatus.DISABLED)
        refresh()
        val fix = manager.location
        if (fix == null || !fix.horizontalAccuracy.isFinite() || fix.horizontalAccuracy < 0) {
            return@withContext LocationClaim(null, null, null, null, null, null, LocationStatus.UNAVAILABLE)
        }
        val ageMs = (-fix.timestamp.timeIntervalSinceNow() * 1000).toLong().coerceAtLeast(0)
        val precise =
            manager.accuracyAuthorization == CLAccuracyAuthorizationFullAccuracy
        LocationClaim(
            latitude = fix.coordinate.useContents { latitude },
            longitude = fix.coordinate.useContents { longitude },
            accuracyM = fix.horizontalAccuracy.toFloat().takeIf { it >= 0 },
            fixAgeMs = ageMs,
            provider = "core-location",
            accuracyLevel = if (precise) "full" else "reduced",
            status = if (precise) LocationStatus.PRECISE else LocationStatus.APPROXIMATE,
            isMocked = fix.sourceInformation?.isSimulatedBySoftware == true,
            mockSignal = fix.sourceInformation?.let { if (it.isSimulatedBySoftware) "MOCKED" else "NOT_INDICATED" },
            fixTimeDevice = fix.timestamp.description,
        ).validate()
    }
}
