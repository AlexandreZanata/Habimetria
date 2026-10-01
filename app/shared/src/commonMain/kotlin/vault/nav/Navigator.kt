package vault.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// App routes. Navigation state lives here, never in screen-local memory,
// so process death, deep links and tests share one source of truth.
sealed interface Route {
    data object Home : Route
    data class Rooms(val inspectionId: String) : Route
    data class Room(val inspectionId: String, val roomId: String, val label: String) : Route
    data class Review(val inspectionId: String) : Route
    data class Seal(val inspectionId: String) : Route
    data class Pdf(val inspectionId: String, val reportId: String, val verifyCode: String) : Route
    data class LocalPdf(val inspectionId: String) : Route
    data class LocalVerify(val inspectionId: String) : Route
    data class LocalOccurrence(val inspectionId: String) : Route
    data class LocalCompare(val exitInspectionId: String) : Route
    data class LocalInspector(val inspectionId: String) : Route
    data class LocalDiff(val exitInspectionId: String) : Route
}

class VaultNavigator(initial: Route = Route.Home) {
    private val stack = ArrayDeque<Route>().also { it.add(initial) }
    private val _route = MutableStateFlow<Route>(initial)
    val route: StateFlow<Route> = _route.asStateFlow()

    val canGoBack: Boolean get() = stack.size > 1

    fun navigate(route: Route) {
        stack.add(route)
        _route.value = route
    }

    // Starting a new inspection resets the journey; back never leaks
    // across inspections.
    fun startInspection(inspectionId: String) {
        stack.clear()
        stack.add(Route.Home)
        stack.add(Route.Rooms(inspectionId))
        _route.value = Route.Rooms(inspectionId)
    }

    fun goBack(): Boolean {
        if (stack.size <= 1) return false
        stack.removeLast()
        _route.value = stack.last()
        return true
    }

    // Back with a floor: resuming straight into a screen leaves a
    // single-route stack where goBack() dies. The floor keeps the
    // bottom Voltar identical to the header back in every entry point:
    // first room → rooms list, rooms list → home.
    fun goBackOr(fallback: Route) {
        if (!goBack()) navigate(fallback)
    }
}
