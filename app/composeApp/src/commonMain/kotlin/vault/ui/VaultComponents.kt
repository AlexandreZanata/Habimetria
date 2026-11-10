package vault.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Shared building blocks. Screens compose these instead of raw
// Columns/Buttons so spacing, type and touch targets stay consistent.

// ScreenColumn is the single content wrapper: spacing and padding live
// here, applied once. Callers add Scaffold innerPadding via modifier.
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// Single heading chokepoint: every screen title announces as a
// heading so screen-reader and switch users land on it first.
@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

// Errors announce politely when they appear: every failure surfaces
// through this chokepoint, so no screen-reader user misses one.
@Composable
fun ErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            Strings.error(message),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text)
    }
}

// Loading announces instead of staying silent: the indeterminate bar
// carries a description so assistive tech reports progress state.
@Composable
fun ProgressStatus(show: Boolean, modifier: Modifier = Modifier) {
    if (show) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth().semantics {
                contentDescription = Strings.loading
            },
        )
    }
}

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null) }
        } else {
            null
        },
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun RecordPanel(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val featured = action != null
    val container = if (featured) colors.inverseSurface else colors.surface
    val ink = if (featured) colors.inverseOnSurface else colors.onSurface
    val iconBackground = if (featured) colors.inversePrimary else colors.surfaceVariant
    val iconInk = if (featured) colors.inverseSurface else colors.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = ink,
        border = BorderStroke(1.dp, if (featured) container else colors.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(shape = MaterialTheme.shapes.small, color = iconBackground) {
                    Icon(
                        icon, contentDescription = null, tint = iconInk,
                        modifier = Modifier.size(42.dp).padding(11.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (action != null) {
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inverseOnSurface,
                        contentColor = colors.inverseSurface,
                        disabledContainerColor = colors.inverseOnSurface.copy(alpha = 0.18f),
                        disabledContentColor = colors.inverseOnSurface.copy(alpha = 0.55f),
                    ),
                ) { Text(action) }
            }
        }
    }
}
