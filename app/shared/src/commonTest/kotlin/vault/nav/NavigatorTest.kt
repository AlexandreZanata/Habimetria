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
}
