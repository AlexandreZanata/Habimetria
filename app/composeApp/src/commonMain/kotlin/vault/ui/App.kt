package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// M4 navigation: Home → Rooms → Room → Review → Seal → PDF.
private sealed interface Route {
    data object Home : Route
    data class Rooms(val inspectionId: String) : Route
    data class Room(val inspectionId: String, val roomId: String, val label: String) : Route
    data class Review(val inspectionId: String) : Route
    data class Seal(val inspectionId: String) : Route
    data class Pdf(val inspectionId: String, val reportId: String, val verifyCode: String) : Route
}

@Composable
fun App(container: AppContainer) {
    var route: Route by remember { mutableStateOf(Route.Home) }
    VaultTheme {
        when (val r = route) {
            Route.Home -> HomeScreen(container, onReady = { route = Route.Rooms(it) })
            is Route.Rooms -> RoomsScreen(
                container = container,
                inspectionId = r.inspectionId,
                onOpenRoom = { roomId, label -> route = Route.Room(r.inspectionId, roomId, label) },
                onReview = { route = Route.Review(r.inspectionId) },
                onBack = { route = Route.Home },
            )
            is Route.Room -> RoomScreen(
                container = container,
                inspectionId = r.inspectionId,
                roomId = r.roomId,
                roomLabel = r.label,
                onBack = { route = Route.Rooms(r.inspectionId) },
            )
            is Route.Review -> ReviewScreen(
                container = container,
                inspectionId = r.inspectionId,
                onSeal = { route = Route.Seal(r.inspectionId) },
                onBack = { route = Route.Rooms(r.inspectionId) },
            )
            is Route.Seal -> SealScreen(
                container = container,
                inspectionId = r.inspectionId,
                onPdf = { reportId, code -> route = Route.Pdf(r.inspectionId, reportId, code) },
                onBack = { route = Route.Review(r.inspectionId) },
            )
            is Route.Pdf -> PdfScreen(
                container = container,
                inspectionId = r.inspectionId,
                reportId = r.reportId,
                verifyCode = r.verifyCode,
                onBack = { route = Route.Seal(r.inspectionId) },
            )
        }
    }
}
