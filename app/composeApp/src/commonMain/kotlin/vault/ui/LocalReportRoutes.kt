package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.ui.settings.SettingsStrings

// Local document destinations share the same offline container.
@Composable
internal fun LocalReportRoutes(r: Route, container: AppContainer, navigator: VaultNavigator) {
    when (r) {
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
                    LocalInspectorScreen(session = local, inspectionId = r.inspectionId)
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
                        picker = container.filePicker,
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
                        picker = container.filePicker,
                        inspectionId = r.inspectionId,
                        onShare = { path, mime -> container.platformPdf.share(path, mime) },
                        onVerify = { navigator.navigate(Route.LocalVerify(r.inspectionId)) },
                        onRoomsShortcut = { navigator.navigate(Route.Rooms(r.inspectionId)) },
                        onReviewShortcut = { navigator.navigate(Route.Review(r.inspectionId)) },
                    )
                } else {
                    ScreenColumn {
                        ErrorText(Strings.pdfSealFirst)
                        VaultButton(text = Strings.back, onClick = { navigator.goBack() })
                    }
                }
            }
        else -> error("Unsupported local report route")
    }
}
