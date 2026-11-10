package vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vault.local.SyncMode
import vault.nav.Destinations
import vault.nav.Route
import vault.nav.VaultNavigator
import vault.nav.destination

// Global chrome: one header (VaultShell) plus one destination switcher.
// Compact windows get a bottom bar with four always-labeled destinations;
// medium/expanded windows get a side rail with the same four. Selection
// follows destination(), so detail screens keep their parent highlighted.
// Selection uses shape and semantics as well as color.

private data class DestinationItem(
    val root: Route,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    DestinationItem(Route.Home, Strings.home, Icons.Outlined.Home),
    DestinationItem(Route.Properties, Strings.properties, Icons.Outlined.Apartment),
    DestinationItem(Route.Inspections, Strings.inspections, Icons.AutoMirrored.Outlined.Assignment),
    DestinationItem(Route.Settings, Strings.settingsTab, Icons.Outlined.Settings),
)

@Composable
fun DestinationsBar(selected: Route, onSelect: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalHabimetriaDarkTheme.current
    val barColor = if (dark) colors.surface else colors.inverseSurface
    val activeBackground = if (dark) colors.primaryContainer else LightNavigationWhite
    val activeInk = if (dark) colors.onPrimaryContainer else colors.inverseSurface
    val inactiveInk = if (dark) colors.onSurfaceVariant else LightNavigationWhite
    Row(
        modifier = Modifier.fillMaxWidth().background(barColor)
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .padding(horizontal = 12.dp, vertical = 6.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        destinations.forEach { item ->
            val active = selected == item.root
            val shape = MaterialTheme.shapes.medium
            val background = if (active) activeBackground else barColor
            val ink = if (active) activeInk else inactiveInk
            Column(
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).clip(shape)
                    .background(background).selectable(active, role = Role.Tab, onClick = { onSelect(item.root) })
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(item.icon, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp))
                Text(item.label, color = ink, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
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
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
        showWordmark = route == Route.Home,
        localMode = container.syncMode == SyncMode.LOCAL_ONLY,
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
