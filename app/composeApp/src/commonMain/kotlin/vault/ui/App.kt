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
                        onStartCapture = { roomId, label ->
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
                val local = container.local
                if (local != null) {
                    LocalRoomScreen(
                        session = local,
                        photoCapture = container.photoCapture,
                        inspectionId = r.inspectionId,
                        roomId = r.roomId,
                        roomLabel = r.label,
                        onReview = { navigator.navigate(Route.Review(r.inspectionId)) },
                        onBack = { navigator.goBack() },
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
                        onBack = { navigator.goBack() },
                    )
                }
            }
            is Route.Review -> VaultShell(Strings.review, true, { navigator.goBack() }) {
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
            is Route.Seal -> VaultShell(Strings.sealTitle, true, { navigator.goBack() }) {
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
            is Route.Pdf -> VaultShell(Strings.reportPdf, true, { navigator.goBack() }) {
                PdfScreen(
                    container = container,
                    inspectionId = r.inspectionId,
                    reportId = r.reportId,
                    verifyCode = r.verifyCode,
                    onBack = { navigator.goBack() },
                )
            }
            is Route.LocalOccurrence -> VaultShell(Strings.occurrenceTitle, true, { navigator.goBack() }) {
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
            is Route.LocalCompare -> VaultShell(Strings.compareTitle, true, { navigator.goBack() }) {
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
            is Route.LocalDiff -> VaultShell(Strings.diffTitle, true, { navigator.goBack() }) {
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
            is Route.LocalInspector -> VaultShell(Strings.inspectorTitle, true, { navigator.goBack() }) {
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
            is Route.LocalVerify -> VaultShell(Strings.verifyTitle, true, { navigator.goBack() }) {
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
            is Route.LocalPdf -> VaultShell(Strings.reportPdf, true, { navigator.goBack() }) {
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
