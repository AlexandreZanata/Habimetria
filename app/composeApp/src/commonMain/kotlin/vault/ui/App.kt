package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// M1 navigation: explicit state, zero navigation deps (lightest option).
private sealed interface Route {
    data object Home : Route
    data class Room(val label: String) : Route
}

@Composable
fun App() {
    var route: Route by remember { mutableStateOf(Route.Home) }
    VaultTheme {
        when (val r = route) {
            Route.Home -> HomeScreen(coverage = 0, onStartRoom = { route = Route.Room("Kitchen") })
            is Route.Room -> RoomScreen(
                roomLabel = r.label,
                shotIndex = 1,
                shotTotal = 4,
                shotHint = "Photograph the whole room",
                onShutter = {},
                onBack = { route = Route.Home },
            )
        }
    }
}
