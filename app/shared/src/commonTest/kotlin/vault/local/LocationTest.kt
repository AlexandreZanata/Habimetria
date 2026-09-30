package vault.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationTest {

    @Test
    fun unavailableTrackerNeverFabricatesCoordinates() = runTest {
        val claim = UnavailableTracker().lastClaim()
        assertEquals(LocationStatus.UNAVAILABLE, claim.status)
        assertNull(claim.latitude)
        assertNull(claim.longitude)
        assertFalse(isStaleClaim(claim.copy(fixAgeMs = 1_000)))
    }

    @Test
    fun stalePolicyFlagsOldAndMissingAges() {
        val fresh = LocationClaim(1.0, 2.0, 5f, 5_000, "gps", "fine", LocationStatus.PRECISE)
        assertFalse(isStaleClaim(fresh))
        assertTrue(isStaleClaim(fresh.copy(fixAgeMs = 31_000)))
        assertTrue(isStaleClaim(fresh.copy(fixAgeMs = null)))
    }

    @Test
    fun preciseWithoutCoordinatesIsRejected() {
        assertFailsWith<LocalValidationException> {
            LocationClaim(null, null, 5f, 0, "gps", "fine", LocationStatus.PRECISE).validate()
        }
        assertFailsWith<LocalValidationException> {
            LocationClaim(1.0, null, 5f, 0, "gps", "fine", LocationStatus.APPROXIMATE).validate()
        }
        // Denied/unavailable legitimately carry nulls.
        LocationClaim(null, null, null, null, null, null, LocationStatus.DENIED).validate()
        LocationClaim(null, null, null, null, null, null, LocationStatus.UNAVAILABLE).validate()
    }
}
