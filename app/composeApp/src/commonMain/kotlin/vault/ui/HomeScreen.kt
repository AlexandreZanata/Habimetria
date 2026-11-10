package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.NavigationProgress
import vault.local.PropertyOverview
import vault.ui.property.HomeHeroBanner
import vault.ui.property.HomePropertyStates
import vault.ui.property.PropertyRemovalHost
import vault.ui.property.RemovalFeedback
import vault.ui.property.startOnProperty

// Home prioritizes new/resume: hero banner entry to the dedicated address
// screen and Continuar for the latest draft. The address form lives
// on Route.NewInspection, never squeezed under these cards. Full
// management lives in Imóveis; this screen keeps cards read-mostly.
// Loading, error and empty are distinct states with retry. No stat cards:
// the banner leads straight into Meus imóveis.
@Composable
fun HomeScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
    onNewInspection: () -> Unit,
    onOpenDetail: (propertyId: String) -> Unit,
    onOpenAllProperties: () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var cards by remember { mutableStateOf<List<PropertyOverview>?>(null) }
    var removing by remember { mutableStateOf<PropertyOverview?>(null) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val local = container.local

    suspend fun load() {
        val repo = local?.repository ?: return
        try {
            // Active picker: archived Properties live in Imóveis → Arquivados.
            cards = repo.propertyOverviews(limit = 100)
                .filter { !it.property.archived }
            loadError = null
            feedback = null
        } catch (e: Exception) {
            loadError = userErrorMessage(e)
        }
    }

    LaunchedEffect(local) { load() }

    fun resume(id: String) {
        scope.launch {
            try {
                local?.navigation?.save(NavigationProgress(id))
                onReady(id)
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
        }
    }

    fun startProperty(propertyId: String) {
        scope.launch {
            busy = true
            error = null
            try {
                startOnProperty(container, propertyId, onReady)
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val showLoading = rememberLoadingVisible(local == null)
        if (local != null) {
            HomeHeroBanner(enabled = !busy, onNewInspection = onNewInspection)
            HomePropertyStates(
                rows = cards,
                loadError = loadError,
                busy = busy,
                onRetry = { scope.launch { load() } },
                onResume = ::resume,
                onNewInspection = ::startProperty,
                onOpenDetail = { onOpenDetail(it.property.id) },
                onOpenAllProperties = onOpenAllProperties,
                onRemove = { removing = it },
            )
        } else if (showLoading) {
            ProgressStatus(true)
            HomeBannerSkeleton()
            PropertyCardSkeleton()
            PropertyCardSkeleton()
        }
        ErrorText(error)
        val done = feedback
        if (done != null) RemovalFeedback(done)
    }

    removing?.let { target ->
        PropertyRemovalHost(
            container = container,
            overview = target,
            onCommitted = { message ->
                scope.launch {
                    load()
                    feedback = message
                    removing = null
                }
            },
            onDismiss = { removing = null },
        )
    }
}


