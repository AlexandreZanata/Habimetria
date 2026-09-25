package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// M3 navigation: Home → Room → Review → Seal.
private sealed interface Route {
    data object Home : Route
    data class Room(val inspectionId: String) : Route
    data class Review(val inspectionId: String) : Route
    data class Seal(val inspectionId: String) : Route
}

@Composable
fun App(container: AppContainer) {
    var route: Route by remember { mutableStateOf(Route.Home) }
    VaultTheme {
        when (val r = route) {
            Route.Home -> HomeScreen(container, onReady = { route = Route.Room(it) })
            is Route.Room -> RoomScreen(
                container, r.inspectionId,
                onReview = { route = Route.Review(r.inspectionId) },
                onBack = { route = Route.Home },
            )
            is Route.Review -> ReviewScreen(
                container, r.inspectionId,
                onSeal = { route = Route.Seal(r.inspectionId) },
                onBack = { route = Route.Room(r.inspectionId) },
            )
            is Route.Seal -> SealScreen(container, r.inspectionId, onBack = { route = Route.Review(r.inspectionId) })
        }
    }
}
