package vault.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.ui.RecordPanel
import vault.ui.Strings

// Report PDF option cards matching the reference layout: a ready-status
// card with the primary share action, then icon rows for the secondary
// actions. Theme-aware surfaces only; no action or disclaimer is dropped.
@Composable
fun PdfStatusCard(versionNumber: Int, shareEnabled: Boolean, onShare: () -> Unit) {
    RecordPanel(
        icon = Icons.Filled.Description,
        title = "${Strings.pdfReadyTitle} · ${Strings.pdfVersionBadge} $versionNumber",
        body = ReportStrings.readyBody,
        action = Strings.sharePdf,
        actionEnabled = shareEnabled,
        onAction = onShare,
    )
}

@Composable
fun PdfOptionCard(
    icon: ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        PdfOptionContent(icon, title, body, enabled)
    }
}

@Composable
internal fun PdfOptionsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun PdfOptionRow(icon: ImageVector, title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    PdfOptionContent(icon, title, body, enabled, Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick))
}

@Composable
private fun PdfOptionContent(
    icon: ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ink = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
    val supportingInk = if (enabled) colors.onSurfaceVariant else ink
    Row(
        modifier = Modifier.fillMaxWidth().then(modifier).heightIn(min = 48.dp).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(MaterialTheme.shapes.small)
                .background(if (enabled) colors.secondaryContainer else colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) colors.onSecondaryContainer else ink, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = supportingInk)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = supportingInk, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PdfSectionHeader(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PdfSectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

object PdfOptionIcons {
    val zip: ImageVector = Icons.Filled.Archive
    val authorship: ImageVector = Icons.Filled.Description
    val external: ImageVector = Icons.Filled.Draw
    val newVersion: ImageVector = Icons.Filled.Refresh
    val offline: ImageVector = Icons.Filled.Shield
}
