package vault.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import vault.ui.ErrorText
import vault.ui.Strings

@Composable
fun SettingsScreen(
    showHomeBanner: Boolean?,
    bannerError: String?,
    onHomeBannerChange: ((Boolean) -> Unit)?,
    onOpenReports: () -> Unit,
    onOpenProblems: () -> Unit,
    onOpenRoomPresets: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Filled.Description,
                    title = Strings.settingsReportsTitle,
                    subtitle = Strings.settingsReportsBody,
                    modifier = Modifier.clickable(onClick = onOpenReports),
                )
                HorizontalDivider()
                SettingsRow(
                    icon = Icons.Filled.Home,
                    title = SettingsStrings.roomPresetsTitle,
                    subtitle = SettingsStrings.roomPresetsBody,
                    modifier = Modifier.clickable(onClick = onOpenRoomPresets),
                )
                HorizontalDivider()
                SettingsRow(
                    icon = Icons.Filled.Build,
                    title = SettingsStrings.problemsTitle,
                    subtitle = SettingsStrings.problemsBody,
                    modifier = Modifier.clickable(onClick = onOpenProblems),
                )
                if (showHomeBanner != null) {
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Outlined.ViewDay,
                        title = SettingsStrings.homeBannerTitle,
                        subtitle = SettingsStrings.homeBannerBody,
                        modifier = Modifier.toggleable(
                            value = showHomeBanner,
                            enabled = onHomeBannerChange != null,
                            role = Role.Switch,
                            onValueChange = { onHomeBannerChange?.invoke(it) },
                        ),
                    ) {
                        Switch(checked = showHomeBanner, onCheckedChange = null, enabled = onHomeBannerChange != null)
                    }
                }
            }
        }
        ErrorText(bannerError)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    trailingContent: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    },
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp).padding(11.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingContent()
    }
}
