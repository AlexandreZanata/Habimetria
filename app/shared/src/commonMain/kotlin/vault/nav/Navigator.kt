package vault.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vault.local.NavigationProgress

// App routes. Navigation state lives here, never in screen-local memory,
// so process death, deep links and tests share one source of truth.
// Home/Properties/Inspections/Reports are the global destination roots;
// PropertyDetail and the inspection journey screens are detail routes.
sealed interface Route {
    data object Home : Route
    data object Properties : Route
    data class PropertyDetail(val propertyId: String) : Route
    data object Inspections : Route
    data object Reports : Route
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

// Persisted destination codes for NavigationProgress.destination.
// "" is legacy/home; unknown codes fall back to Home.
object Destinations {
    const val HOME = "home"
    const val PROPERTIES = "properties"
    const val INSPECTIONS = "inspections"
    const val REPORTS = "reports"

    fun rootFor(code: String): Route = when (code) {
        PROPERTIES -> Route.Properties
        INSPECTIONS -> Route.Inspections
        REPORTS -> Route.Reports
        else -> Route.Home
    }
}

fun Route.isDestinationRoot(): Boolean = when (this) {
    Route.Home, Route.Properties, Route.Inspections, Route.Reports -> true
    else -> false
}

// Repository facts needed to validate a stored trail. Callers read these
// from LocalVaultRepository; navigation stays sync and JVM-testable.
data class RestoreFacts(
    val inspectionExists: Boolean,
    val inspectionSealed: Boolean,
    val roomExists: Boolean,
    val propertyExists: Boolean,
)

// Restart trail from stored progress: stable IDs only, revalidated.
// Removed targets fall back to their list root, SEALED never restores
// an edit route, and stale rooms/property links are dropped.
fun trailFor(progress: NavigationProgress, facts: RestoreFacts): List<Route> {
    val root = Destinations.rootFor(progress.destination)
    val inspectionId = progress.inspectionId?.takeIf { it.isNotBlank() }
    val propertyId = progress.propertyId?.takeIf { it.isNotBlank() }
    if (inspectionId == null) {
        if (propertyId != null) {
            return if (facts.propertyExists) {
                listOf(Route.Properties, Route.PropertyDetail(propertyId))
            } else {
                listOf(Route.Properties)
            }
        }
        return listOf(root)
    }
    if (!facts.inspectionExists) return listOf(root)
    if (facts.inspectionSealed) return listOf(Route.Home, Route.Review(inspectionId))
    val roomId = progress.roomId?.takeIf { it.isNotBlank() }
    val label = progress.roomLabel?.takeIf { it.isNotBlank() }
    if (roomId != null && label != null && facts.roomExists) {
        return listOf(Route.Home, Route.Rooms(inspectionId), Route.Room(inspectionId, roomId, label))
    }
    return listOf(Route.Home, Route.Rooms(inspectionId))
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

    // Tab selection never grows the stack: re-selecting the current root
    // is a no-op, switching roots replaces the trail. 50 switches stay
    // at depth 1; Back never replays the tab-switch sequence.
    fun selectDestination(route: Route) {
        require(route.isDestinationRoot()) { "selectDestination needs a destination root" }
        if (stack.lastOrNull() == route) return
        stack.clear()
        stack.add(route)
        _route.value = route
    }

    // Validated restart replaces the whole trail. Empty trails fall back
    // to Home so a navigator never ends up without a bottom route.
    fun restoreStack(routes: List<Route>) {
        stack.clear()
        if (routes.isEmpty()) stack.add(Route.Home) else stack.addAll(routes)
        _route.value = stack.last()
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

    // Sealed inspections are read-only: edit screens bounce to review
    // instead of trapping back behind dead routes. Trailing Rooms/Room
    // of the same inspection are dropped so Voltar from review never
    // lands back on a sealed editor.
    fun openSealedReview(inspectionId: String) {
        while (true) {
            val last = stack.lastOrNull() ?: break
            val sameInspection = when (last) {
                is Route.Rooms -> last.inspectionId == inspectionId
                is Route.Room -> last.inspectionId == inspectionId
                else -> false
            }
            if (!sameInspection) break
            stack.removeLast()
        }
        if (stack.isEmpty()) stack.add(Route.Home)
        val review = Route.Review(inspectionId)
        stack.add(review)
        _route.value = review
    }
}
