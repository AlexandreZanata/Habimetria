package vault.local

// Shutter-time location behind a shared interface. The tracker only reads
// the cached fix: capture never waits for GPS and never loses a photo to
// it. Absent fixes stay absent — never another photo's fix, never (0,0).
enum class LocationStatus {
    PRECISE, APPROXIMATE, DENIED, UNAVAILABLE
}

data class LocationClaim(
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val fixAgeMs: Long?,
    val provider: String?,
    val accuracyLevel: String?,
    val status: LocationStatus,
    val isMocked: Boolean = false,
    val mockSignal: String? = null,
    val fixTimeDevice: String? = null,
) {
    // A claim is only as strong as its coordinates: precise/approximate
    // without coordinates is a programming error, caught before storage.
    fun validate(): LocationClaim {
        if ((status == LocationStatus.PRECISE || status == LocationStatus.APPROXIMATE) &&
            (latitude == null || longitude == null)
        ) {
            throw LocalValidationException("precise/approximate claim without coordinates")
        }
        require((latitude == null) == (longitude == null)) { "incomplete coordinates" }
        require(latitude == null || (latitude.isFinite() && latitude in -90.0..90.0)) { "invalid latitude" }
        require(longitude == null || (longitude.isFinite() && longitude in -180.0..180.0)) { "invalid longitude" }
        require(accuracyM == null || (accuracyM.isFinite() && accuracyM >= 0)) { "invalid accuracy" }
        require(fixAgeMs == null || fixAgeMs >= 0) { "invalid fix age" }
        require(mockSignal == null || mockSignal in listOf("MOCKED", "NOT_INDICATED")) { "invalid mock signal" }
        return this
    }
}

// Fixes older than this are recorded with their age, never refreshed
// behind the shutter's back and never presented as fresh.
const val STALE_FIX_AGE_MS = 30_000L

fun isStaleClaim(claim: LocationClaim): Boolean =
    claim.fixAgeMs == null || claim.fixAgeMs > STALE_FIX_AGE_MS

interface LocationTracker {
    suspend fun lastClaim(): LocationClaim
}

expect fun platformLocationTracker(): LocationTracker
