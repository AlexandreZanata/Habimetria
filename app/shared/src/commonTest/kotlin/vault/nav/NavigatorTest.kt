package vault.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vault.local.NavigationProgress

private val alive = RestoreFacts(
    inspectionExists = true,
    inspectionSealed = false,
    roomExists = true,
    propertyExists = true,
)

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

    @Test
    fun destinationSelectionStaysBounded() {
        val nav = VaultNavigator()
        val roots = listOf(Route.Properties, Route.Inspections, Route.Settings, Route.Home)
        repeat(50) { nav.selectDestination(roots[it % roots.size]) }
        assertEquals(Route.Inspections, nav.route.value)
        assertFalse(nav.canGoBack)

        nav.selectDestination(Route.Inspections)
        assertFalse(nav.canGoBack)

        nav.navigate(Route.PropertyDetail("p1"))
        nav.selectDestination(Route.Settings)
        assertEquals(Route.Settings, nav.route.value)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun startInspectionDropsPropertyDetail() {
        val nav = VaultNavigator()
        nav.selectDestination(Route.Properties)
        nav.navigate(Route.PropertyDetail("p1"))
        nav.startInspection("insB")
        assertEquals(Route.Rooms("insB"), nav.route.value)
        assertTrue(nav.goBack())
        assertEquals(Route.Home, nav.route.value)
    }

    @Test
    fun directEntryBackParity() {
        val nav = VaultNavigator(Route.PropertyDetail("p1"))
        nav.goBackOr(Route.Properties)
        assertEquals(Route.Properties, nav.route.value)
    }

    @Test
    fun sealedRestoreNeverOpensEditor() {
        val progress = NavigationProgress("ins1", "r1", "Sala")
        val trail = trailFor(progress, alive.copy(inspectionSealed = true))
        assertEquals(listOf(Route.Home, Route.Review("ins1")), trail)
    }

    @Test
    fun removedTargetsFallBackToListRoots() {
        val goneInspection = NavigationProgress("gone", "r1", "Sala")
        assertEquals(
            listOf(Route.Home),
            trailFor(goneInspection, alive.copy(inspectionExists = false)),
        )
        val staleRoom = NavigationProgress("ins1", "gone", "Sala")
        assertEquals(
            listOf(Route.Home, Route.Rooms("ins1")),
            trailFor(staleRoom, alive.copy(roomExists = false)),
        )
        val keptProperty = NavigationProgress(propertyId = "p1")
        assertEquals(
            listOf(Route.Properties, Route.PropertyDetail("p1")),
            trailFor(keptProperty, alive),
        )
        assertEquals(
            listOf(Route.Properties),
            trailFor(keptProperty, alive.copy(propertyExists = false)),
        )
    }

    @Test
    fun legacyProgressRestoresJourney() {
        val rooms = trailFor(NavigationProgress("ins1"), alive.copy(roomExists = false))
        assertEquals(listOf(Route.Home, Route.Rooms("ins1")), rooms)
        val room = trailFor(NavigationProgress("ins1", "r1", "Sala"), alive)
        assertEquals(
            listOf(Route.Home, Route.Rooms("ins1"), Route.Room("ins1", "r1", "Sala")),
            room,
        )
        assertEquals(listOf(Route.Home), trailFor(NavigationProgress(), alive))
    }

    @Test
    fun everyRouteMapsToOneDestination() {
        assertEquals(Route.Home, Route.Home.destination())
        assertEquals(Route.Properties, Route.Properties.destination())
        assertEquals(Route.Properties, Route.PropertyDetail("p1").destination())
        assertEquals(Route.Inspections, Route.Inspections.destination())
        assertEquals(Route.Settings, Route.Reports.destination())
        assertEquals(Route.Settings, Route.Settings.destination())
        val journey: List<Route> = listOf(
            Route.Rooms("i"),
            Route.Room("i", "r", "Sala"),
            Route.Review("i"),
            Route.Seal("i"),
            Route.LocalPdf("i"),
            Route.Pdf("i", "rep", "code"),
            Route.LocalVerify("i"),
            Route.LocalOccurrence("i"),
            Route.LocalCompare("i"),
            Route.LocalInspector("i"),
            Route.LocalDiff("i"),
        )
        journey.forEach { assertEquals(Route.Inspections, it.destination()) }
    }

    @Test
    fun destinationCodesRoundTrip() {
        assertEquals(Destinations.PROPERTIES, Destinations.codeFor(Route.Properties))
        assertEquals(Destinations.INSPECTIONS, Destinations.codeFor(Route.Inspections))
        assertEquals(Destinations.SETTINGS, Destinations.codeFor(Route.Settings))
        assertEquals(Destinations.HOME, Destinations.codeFor(Route.Reports))
        assertEquals(Destinations.HOME, Destinations.codeFor(Route.Home))
        assertEquals(Route.Properties, Destinations.rootFor(Destinations.PROPERTIES))
        assertEquals(Route.Settings, Destinations.rootFor(Destinations.REPORTS))
        assertEquals(Route.Home, Destinations.rootFor("unknown"))
        assertEquals(Route.Home, Destinations.rootFor(""))
    }
}
