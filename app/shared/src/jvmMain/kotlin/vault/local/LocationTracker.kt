package vault.local

// Desktop/test path: no location hardware is assumed. Every claim is
// UNAVAILABLE with null coordinates — never (0,0), never fabricated.
actual fun platformLocationTracker(): LocationTracker = UnavailableTracker()

class UnavailableTracker : LocationTracker {
    override suspend fun lastClaim(): LocationClaim =
        LocationClaim(null, null, null, null, null, null, LocationStatus.UNAVAILABLE)
}
