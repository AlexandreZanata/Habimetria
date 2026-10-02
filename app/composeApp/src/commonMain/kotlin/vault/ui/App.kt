package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.ui.settings.SettingsStrings

// App owns the navigator; screens receive callbacks and stay stateless
// about navigation. One shell per route: insets applied once at the edge.
@Composable
fun App(container: AppContainer, navigator: VaultNavigator = remember { VaultNavigator() }) {
    val route by navigator.route.collectAsState()
    VaultTheme {
        when (val r = route) {
            Route.Home -> ChromeShell(r, container, navigator, Strings.home, false, {}) {
                HomeScreen(
                    container,
                    onReady = { navigator.startInspection(it) },
                    onNewInspection = { navigator.navigate(Route.NewInspection) },
                    onOpenDetail = { navigator.navigate(Route.PropertyDetail(it)) },
                )
            }
            Route.NewInspection -> ChromeShell(r, container, navigator, Strings.newInspection, true, {
                navigator.goBackOr(Route.Home)
            }) {
                vault.ui.property.NewInspectionScreen(
                    container,
                    onReady = { navigator.startInspection(it) },
                )
            }
            // Imóveis (U04), Vistorias and Relatórios (U05) are
            // dedicated destinations with offline-first lists.
            Route.Properties -> ChromeShell(r, container, navigator, Strings.properties, false, {}) {
                vault.ui.property.PropertyListScreen(
                    container = container,
                    onOpenDetail = { navigator.navigate(Route.PropertyDetail(it)) },
                    onReady = { navigator.startInspection(it) },
                )
            }
            is Route.PropertyDetail -> ChromeShell(r, container, navigator, Strings.properties, true, {
                navigator.goBackOr(Route.Properties)
            }) {
                vault.ui.property.PropertyDetailScreen(
                    container = container,
                    propertyId = r.propertyId,
                    onResume = { navigator.startInspection(it) },
                    onReview = { navigator.openSealedReview(it) },
                    onNewInspection = { navigator.startInspection(it) },
                )
            }
            Route.Inspections -> ChromeShell(r, container, navigator, Strings.inspections, false, {}) {
                vault.ui.inspection.InspectionListScreen(
                    container = container,
                    onResume = { navigator.startInspection(it) },
                    onReview = { navigator.openSealedReview(it) },
                )
            }
            Route.Settings -> ChromeShell(r, container, navigator, Strings.settings, false, {}) {
                vault.ui.settings.SettingsScreen(
                    onOpenReports = { navigator.navigate(Route.Reports) },
                    onOpenProblems = { navigator.navigate(Route.FindingProblems) },
                )
            }
            Route.FindingProblems -> ChromeShell(r, container, navigator, SettingsStrings.problemsTitle, true, {
                navigator.goBackOr(Route.Settings)
            }) {
                vault.ui.settings.FindingProblemsScreen(container)
            }
            Route.Reports -> ChromeShell(r, container, navigator, Strings.reports, true, {
                navigator.goBackOr(Route.Settings)
            }) {
                vault.ui.report.ReportListScreen(
                    container = container,
                    onOpenPdf = { navigator.navigate(Route.LocalPdf(it)) },
                    onVerify = { navigator.navigate(Route.LocalVerify(it)) },
                    onGenerate = { navigator.navigate(Route.LocalPdf(it)) },
                )
            }
            is Route.Rooms -> ChromeShell(r, container, navigator, Strings.rooms, true, { navigator.goBackOr(Route.Home) }) {
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
                        onStartCapture = { roomId, label ->
                            local.navigation.save(
                                vault.local.NavigationProgress(r.inspectionId, roomId, label),
                            )
                            navigator.navigate(Route.Room(r.inspectionId, roomId, label))
                        },
                        onSealed = { navigator.openSealedReview(r.inspectionId) },
                    )
                } else {
                    RoomsScreen(
                        container = container,
                        inspectionId = r.inspectionId,
                        onOpenRoom = { roomId, label ->
                            navigator.navigate(Route.Room(r.inspectionId, roomId, label))
                        },
                        onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                        onBack = { navigator.goBackOr(Route.Home) },
                    )
                }
            }
            is Route.Room -> ChromeShell(r, container, navigator, r.label, true, { navigator.goBackOr(Route.Rooms(r.inspectionId)) }) {
                val local = container.local
                if (local != null) {
                    LocalRoomScreen(
                        session = local,
                        photoCapture = container.photoCapture,
                        inspectionId = r.inspectionId,
                        roomId = r.roomId,
                        roomLabel = r.label,
                        onOpenRoom = { nextId, nextLabel ->
                            local.navigation.save(
                                vault.local.NavigationProgress(r.inspectionId, nextId, nextLabel),
                            )
                            navigator.navigate(Route.Room(r.inspectionId, nextId, nextLabel))
                        },
                        onSealed = { navigator.openSealedReview(r.inspectionId) },
                        onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                        onRoomsShortcut = { navigator.navigate(Route.Rooms(r.inspectionId)) },
                    )
                } else {
                    RoomScreen(
                        container = container,
                        inspectionId = r.inspectionId,
                        roomId = r.roomId,
                        roomLabel = r.label,
                        onOpenRoom = { nextId, nextLabel ->
                            navigator.navigate(Route.Room(r.inspectionId, nextId, nextLabel))
                        },
                        onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                        onBack = { navigator.goBackOr(Route.Rooms(r.inspectionId)) },
                    )
                }
            }
            is Route.Review, is Route.Seal, is Route.Pdf -> InspectionCompletionRoutes(r, container, navigator)
            else -> LocalReportRoutes(r, container, navigator)
        }
    }
}
