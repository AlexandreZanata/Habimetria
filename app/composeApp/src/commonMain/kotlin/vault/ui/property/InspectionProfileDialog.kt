package vault.ui.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import vault.local.NavigationProgress
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.VaultFormDialog
import vault.ui.userErrorMessage

@Composable
internal fun InspectionProfileDialog(
    container: AppContainer,
    propertyId: String,
    onReady: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var technical by rememberSaveable(propertyId) { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun start(profileIsTechnical: Boolean) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                if (profileIsTechnical) {
                    val session = requireNotNull(container.local)
                    val inspection = session.repository.createTechnicalInspection(propertyId)
                    session.navigation.save(NavigationProgress(inspection.id))
                    onReady(inspection.id)
                } else {
                    startOnProperty(container, propertyId, onReady)
                }
                onClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    VaultFormDialog(onDismissRequest = { if (!busy) onClose() }) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    PropertyStrings.profileTitle,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onClose, enabled = !busy) {
                    Icon(Icons.Filled.Close, contentDescription = Strings.close)
                }
            }
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    PropertyStrings.profileHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileOption(
                        PropertyStrings.rentalMode,
                        PropertyStrings.rentalModeHint,
                        Icons.Outlined.Key,
                        selected = technical == false,
                        enabled = !busy,
                        onClick = { technical = false },
                    )
                    ProfileOption(
                        PropertyStrings.technicalMode,
                        PropertyStrings.technicalModeHint,
                        Icons.AutoMirrored.Outlined.Assignment,
                        selected = technical == true,
                        enabled = !busy,
                        onClick = { technical = true },
                    )
                }
                ErrorText(error)
            }
            VaultButton(
                text = if (busy) Strings.saving else Strings.startInspection,
                enabled = technical != null && !busy,
                onClick = { technical?.let(::start) },
            )
        }
    }
}

@Composable
private fun ProfileOption(
    title: String,
    body: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ink = if (selected) colors.inverseOnSurface else colors.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.inverseSurface else colors.surface,
        contentColor = ink,
        border = BorderStroke(1.dp, if (selected) colors.inverseSurface else colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ).heightIn(min = 72.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) colors.inverseOnSurface else colors.surfaceVariant,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) colors.inverseSurface else colors.primary,
                    modifier = Modifier.size(40.dp).padding(9.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) ink else colors.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = ink, unselectedColor = colors.onSurfaceVariant),
            )
        }
    }
}
