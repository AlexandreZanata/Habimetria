package vault.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import vault.nav.Destinations
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.nav.destination

// Global chrome: one header (VaultShell) plus one destination switcher.
// Compact windows get a bottom bar with four always-labeled destinations;
// medium/expanded windows get a side rail with the same four. Selection
// follows destination(), so detail screens keep their parent highlighted.
// Selected state uses the theme indicator pill, label and semantics as
// well as green; every destination carries a minimalist Material icon
// (Apache 2.0) next to its label.

private data class DestinationItem(
    val root: Route,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    DestinationItem(Route.Home, Strings.home, Icons.Filled.Home),
    DestinationItem(Route.Properties, Strings.properties, Icons.Filled.Apartment),
    DestinationItem(Route.Inspections, Strings.inspections, Icons.AutoMirrored.Filled.Assignment),
    DestinationItem(Route.Reports, Strings.reports, Icons.Filled.Description),
)

@Composable
fun DestinationsBar(selected: Route, onSelect: (Route) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        destinations.forEach { item ->
            NavigationBarItem(
                selected = selected == item.root,
                onClick = { onSelect(item.root) },
                icon = { Icon(item.icon, contentDescription = null) },
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
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        destinations.forEach { item ->
            NavigationRailItem(
                selected = selected == item.root,
                onClick = { onSelect(item.root) },
                icon = { Icon(item.icon, contentDescription = null) },
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
