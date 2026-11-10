package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.ui.review.ReviewInteractionScope
import vault.ui.settings.SettingsStrings

@Composable
internal fun InspectionCompletionRoutes(r: Route, container: AppContainer, navigator: VaultNavigator) {
    when (r) {
            is Route.Review -> ChromeShell(r, container, navigator, Strings.review, true, { navigator.goBack() }) {
                val local = container.local
                if (local != null) {
                    ReviewInteractionScope {
                        LocalReviewScreen(
                            container = container,
                            session = local,
                            inspectionId = r.inspectionId,
                            onSeal = { navigator.navigate(Route.Seal(r.inspectionId)) },
                            onCompare = { navigator.navigate(Route.LocalCompare(r.inspectionId)) },
                            onInspector = { navigator.navigate(Route.LocalInspector(r.inspectionId)) },
                            onRoomsShortcut = { navigator.navigate(Route.Rooms(r.inspectionId)) },
                        )
                    }
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
                        onRoomsShortcut = { navigator.navigate(Route.Rooms(r.inspectionId)) },
                        onReviewShortcut = { navigator.navigate(Route.Review(r.inspectionId)) },
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
        else -> error("Unsupported inspection completion route")
    }
}
