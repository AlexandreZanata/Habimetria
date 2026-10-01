package vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import vault.nav.Destinations
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.nav.destination

// Global chrome: one header (VaultShell) plus one destination switcher.
// Compact windows get a bottom bar with four always-labeled destinations;
// medium/expanded windows get a side rail with the same four. Selection
// follows destination(), so detail screens keep their parent highlighted.
// Selected state uses the theme indicator pill, label and semantics as
// well as green; the dot marker differs by shape (filled vs hollow).

private data class DestinationItem(val root: Route, val label: String)

private val destinations = listOf(
    DestinationItem(Route.Home, Strings.home),
    DestinationItem(Route.Properties, Strings.properties),
    DestinationItem(Route.Inspections, Strings.inspections),
    DestinationItem(Route.Reports, Strings.reports),
)

@Composable
private fun DestinationDot(selected: Boolean) {
    val base = Modifier.size(10.dp).clip(CircleShape)
    Box(
        modifier = if (selected) {
            base.background(MaterialTheme.colorScheme.primary)
        } else {
            base.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
        },
    )
}

@Composable
fun DestinationsBar(selected: Route, onSelect: (Route) -> Unit) {
    NavigationBar {
        destinations.forEach { item ->
            NavigationBarItem(
                selected = selected == item.root,
                onClick = { onSelect(item.root) },
                icon = { DestinationDot(selected == item.root) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
fun DestinationsRail(selected: Route, onSelect: (Route) -> Unit) {
    NavigationRail {
        destinations.forEach { item ->
            NavigationRailItem(
                selected = selected == item.root,
                onClick = { onSelect(item.root) },
                icon = { DestinationDot(selected == item.root) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

// Header subtitle: property nickname (or address) behind the current
// property/inspection route. Empty when unknown or remote-only; screens
// never show a fabricated property name.
@Composable
fun rememberChromeSubtitle(route: Route, container: AppContainer): String {
    var subtitle by remember(route) { mutableStateOf("") }
    val local = container.local
    LaunchedEffect(route, local) {
        subtitle = try {
            val repo = local?.repository
            val property = when (route) {
                is Route.PropertyDetail -> repo?.listProperties()?.firstOrNull { it.id == route.propertyId }
                is Route.Rooms -> repo?.propertyFor(route.inspectionId)
                is Route.Room -> repo?.propertyFor(route.inspectionId)
                is Route.Review -> repo?.propertyFor(route.inspectionId)
                is Route.Seal -> repo?.propertyFor(route.inspectionId)
                is Route.Pdf -> repo?.propertyFor(route.inspectionId)
                is Route.LocalPdf -> repo?.propertyFor(route.inspectionId)
                is Route.LocalVerify -> repo?.propertyFor(route.inspectionId)
                is Route.LocalOccurrence -> repo?.propertyFor(route.inspectionId)
                is Route.LocalCompare -> repo?.propertyFor(route.exitInspectionId)
                is Route.LocalInspector -> repo?.propertyFor(route.inspectionId)
                is Route.LocalDiff -> repo?.propertyFor(route.exitInspectionId)
                else -> null
            }
            property?.nickname?.takeIf { it.isNotBlank() } ?: property?.address.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
    return subtitle
}

// ChromeShell is the single App wiring point: it derives the destination,
// subtitle and tab persistence from the route and delegates chrome to
// VaultShell. Tab switches persist the destination code without touching
// stored inspection/room IDs, so resume keeps its place.
@Composable
fun ChromeShell(
    route: Route,
    container: AppContainer,
    navigator: VaultNavigator,
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val subtitle = rememberChromeSubtitle(route, container)
    VaultShell(
        title = title,
        canGoBack = canGoBack,
        onBack = onBack,
        destination = route.destination(),
        subtitle = subtitle,
        onSelect = { dest ->
            val local = container.local
            if (local != null) {
                val current = local.navigation.load()
                local.navigation.save(current.copy(destination = Destinations.codeFor(dest)))
            }
            navigator.selectDestination(dest)
        },
        content = content,
    )
}
