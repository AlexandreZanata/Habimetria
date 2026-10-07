package vault.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    @Test
    fun pushAndBackThroughJourney() {
        val nav = VaultNavigator()
        assertEquals(Route.Home, nav.route.value)
        assertFalse(nav.canGoBack)

        nav.startInspection("ins1")
        assertEquals(Route.Rooms("ins1"), nav.route.value)
        nav.navigate(Route.Room("ins1", "r1", "Sala"))
        nav.navigate(Route.Review("ins1"))
        assertTrue(nav.canGoBack)

        assertTrue(nav.goBack())
        assertEquals(Route.Room("ins1", "r1", "Sala"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Rooms("ins1"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Home, nav.route.value)
        assertFalse(nav.goBack())
    }

    @Test
    fun inspectorRoutePushAndBack() {
        val nav = VaultNavigator()
        nav.startInspection("ins9")
        nav.navigate(Route.Review("ins9"))
        nav.navigate(Route.LocalInspector("ins9"))
        assertEquals(Route.LocalInspector("ins9"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Review("ins9"), nav.route.value)
    }

    @Test
    fun diffRoutePushAndBack() {
        val nav = VaultNavigator()
        nav.startInspection("ins9")
        nav.navigate(Route.LocalCompare("ins9"))
        nav.navigate(Route.LocalDiff("ins9"))
        assertEquals(Route.LocalDiff("ins9"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.LocalCompare("ins9"), nav.route.value)
    }

    @Test
    fun newInspectionResetsStack() {
        val nav = VaultNavigator()
        nav.startInspection("a")
        nav.navigate(Route.Review("a"))
        nav.startInspection("b")
        assertEquals(Route.Rooms("b"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Home, nav.route.value)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun backOrFallsBackOnSingleRouteStack() {
        // Resume straight into a room: stack holds one route.
        val resumed = VaultNavigator(Route.Room("ins1", "r1", "Sala"))
        resumed.goBackOr(Route.Rooms("ins1"))
        assertEquals(Route.Rooms("ins1"), resumed.route.value)

        // Normal stack pops instead of jumping to the fallback.
        val nav = VaultNavigator()
        nav.startInspection("ins1")
        nav.navigate(Route.Room("ins1", "r1", "Sala"))
        nav.navigate(Route.Room("ins1", "r2", "Cozinha"))
        nav.goBackOr(Route.Rooms("ins1"))
        assertEquals(Route.Room("ins1", "r1", "Sala"), nav.route.value)
    }

    @Test
    fun sealedReviewDropsDeadEditRoutes() {
        val nav = VaultNavigator()
        nav.startInspection("ins1")
        nav.navigate(Route.Room("ins1", "r1", "Sala"))
        nav.openSealedReview("ins1")
        assertEquals(Route.Review("ins1"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Home, nav.route.value)

        // Single-route resume never traps back.
        val resumed = VaultNavigator(Route.Room("ins1", "r1", "Sala"))
        resumed.openSealedReview("ins1")
        assertEquals(Route.Review("ins1"), resumed.route.value)
        assertTrue(resumed.goBack())
        assertEquals(Route.Home, resumed.route.value)

        // Other inspections are left alone.
        val other = VaultNavigator()
        other.startInspection("ins2")
        other.openSealedReview("ins1")
        assertEquals(Route.Review("ins1"), other.route.value)
        assertTrue(other.goBack())
        assertEquals(Route.Rooms("ins2"), other.route.value)
    }
}
