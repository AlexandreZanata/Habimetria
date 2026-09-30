package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import vault.nav.Route
import vault.nav.VaultNavigator

// App owns the navigator; screens receive callbacks and stay stateless
// about navigation. One shell per route: insets applied once at the edge.
@Composable
fun App(container: AppContainer, navigator: VaultNavigator = remember { VaultNavigator() }) {
    val route by navigator.route.collectAsState()
    VaultTheme {
        when (val r = route) {
            Route.Home -> VaultShell(Strings.appName, false, {}) {
                HomeScreen(container, onReady = { navigator.startInspection(it) })
            }
            is Route.Rooms -> VaultShell(Strings.rooms, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalRoomsScreen(
                        session = local,
                        inspectionId = r.inspectionId,
                        onOpenRoom = { roomId, label ->
                            local.navigation.save(
                                vault.local.NavigationProgress(r.inspectionId, roomId, label),
                            )
                            navigator.navigate(Route.Room(r.inspectionId, roomId, label))
                        },
                        onBack = { navigator.goBack() },
                    )
                } else {
                    RoomsScreen(
                        container = container,
                        inspectionId = r.inspectionId,
                        onOpenRoom = { roomId, label ->
                            navigator.navigate(Route.Room(r.inspectionId, roomId, label))
                        },
                        onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                        onBack = { navigator.goBack() },
                    )
                }
            }
            is Route.Room -> VaultShell(r.label, true, { navigator.goBack() }) {
                RoomScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    roomId = r.roomId,
                    roomLabel = r.label,
                    onOpenRoom = { nextId, nextLabel ->
                        navigator.navigate(Route.Room(r.inspectionId, nextId, nextLabel))
                    },
                    onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                    onBack = { navigator.goBack() },
                )
            }
            is Route.Review -> VaultShell(Strings.review, true, { navigator.goBack() }) {
                ReviewScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    onSeal = { navigator.navigate(Route.Seal(r.inspectionId)) },
                    onBack = { navigator.goBack() },
                )
            }
            is Route.Seal -> VaultShell(Strings.sealTitle, true, { navigator.goBack() }) {
                SealScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    onPdf = { reportId, code ->
                        navigator.navigate(Route.Pdf(r.inspectionId, reportId, code))
                    },
                    onBack = { navigator.goBack() },
                )
            }
            is Route.Pdf -> VaultShell(Strings.reportPdf, true, { navigator.goBack() }) {
                PdfScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    reportId = r.reportId,
                    verifyCode = r.verifyCode,
                    onBack = { navigator.goBack() },
                )
            }
        }
    }
}
