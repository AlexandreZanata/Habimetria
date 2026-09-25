package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// M2 navigation: Home → Room. Review + seal arrive in M3.
private sealed interface Route {
    data object Home : Route
    data class Room(val inspectionId: String) : Route
}

@Composable
fun App(container: AppContainer) {
    var route: Route by remember { mutableStateOf(Route.Home) }
    VaultTheme {
        when (val r = route) {
            Route.Home -> HomeScreen(container, onReady = { route = Route.Room(it) })
            is Route.Room -> RoomScreen(container, r.inspectionId, onBack = { route = Route.Home })
        }
    }
}
