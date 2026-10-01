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
            Route.Home -> ChromeShell(r, container, navigator, Strings.home, false, {}) {
                HomeScreen(container, onReady = { navigator.startInspection(it) })
            }
            // Imóveis owns management/history (U04); inspection/report
            // destinations arrive in U05 and reuse Home until then.
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
                HomeScreen(container, onReady = { navigator.startInspection(it) })
            }
            Route.Reports -> ChromeShell(r, container, navigator, Strings.reports, false, {}) {
                HomeScreen(container, onReady = { navigator.startInspection(it) })
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
                        onBack = { navigator.goBackOr(Route.Home) },
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
                        onBack = { navigator.goBackOr(Route.Rooms(r.inspectionId)) },
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
            is Route.Review -> ChromeShell(r, container, navigator, Strings.review, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalReviewScreen(
                        container = container,
                        session = local,
                        inspectionId = r.inspectionId,
                        onSeal = { navigator.navigate(Route.Seal(r.inspectionId)) },
                        onCompare = { navigator.navigate(Route.LocalCompare(r.inspectionId)) },
                        onInspector = { navigator.navigate(Route.LocalInspector(r.inspectionId)) },
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ReviewScreen(
                        container = container,
                        inspectionId = r.inspectionId,
                        onSeal = { navigator.navigate(Route.Seal(r.inspectionId)) },
                        onBack = { navigator.goBack() },
                    )
                }
            }
            is Route.Seal -> ChromeShell(r, container, navigator, Strings.sealTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalSealScreen(
                        session = local,
                        inspectionId = r.inspectionId,
                        onPdf = { navigator.navigate(Route.LocalPdf(r.inspectionId)) },
                        onOccurrence = { navigator.navigate(Route.LocalOccurrence(r.inspectionId)) },
                        onExitReady = { navigator.startInspection(it) },
                        onBack = { navigator.goBack() },
                    )
                } else {
                    SealScreen(
                        container = container,
                        inspectionId = r.inspectionId,
                        onPdf = { reportId, code ->
                            navigator.navigate(Route.Pdf(r.inspectionId, reportId, code))
                        },
                        onBack = { navigator.goBack() },
                    )
                }
            }
            is Route.Pdf -> ChromeShell(r, container, navigator, Strings.reportPdf, true, { navigator.goBack() }) {
                PdfScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    reportId = r.reportId,
                    verifyCode = r.verifyCode,
                    onBack = { navigator.goBack() },
                )
            }
            is Route.LocalOccurrence -> ChromeShell(r, container, navigator, Strings.occurrenceTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalOccurrenceScreen(
                        session = local,
                        photoCapture = container.photoCapture,
                        inspectionId = r.inspectionId,
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
            is Route.LocalCompare -> ChromeShell(r, container, navigator, Strings.compareTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalCompareScreen(
                        session = local,
                        exitInspectionId = r.exitInspectionId,
                        onDiff = { navigator.navigate(Route.LocalDiff(r.exitInspectionId)) },
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
            is Route.LocalDiff -> ChromeShell(r, container, navigator, Strings.diffTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalDiffScreen(
                        session = local,
                        exitInspectionId = r.exitInspectionId,
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
            is Route.LocalInspector -> ChromeShell(r, container, navigator, Strings.inspectorTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalInspectorScreen(
                        session = local,
                        inspectionId = r.inspectionId,
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
            is Route.LocalVerify -> ChromeShell(r, container, navigator, Strings.verifyTitle, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    LocalVerifyScreen(
                        session = local,
                        inspectionId = r.inspectionId,
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
            is Route.LocalPdf -> ChromeShell(r, container, navigator, Strings.reportPdf, true, { navigator.goBack() }) {
                val local = container.local
                val renderer = container.planRenderer
                if (local != null && renderer != null) {
                    LocalPdfScreen(
                        session = local,
                        renderer = renderer,
                        inspectionId = r.inspectionId,
                        onSharePdf = { path -> container.platformPdf.share(path, "application/pdf") },
                        onVerify = { navigator.navigate(Route.LocalVerify(r.inspectionId)) },
                        onBack = { navigator.goBack() },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
        }
    }
}
