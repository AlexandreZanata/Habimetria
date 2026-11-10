package vault.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vault.ui.Strings

@Composable
fun PdfJourneySegments(onRooms: () -> Unit, onReview: () -> Unit, activeReview: Boolean = false) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        JourneySegment(Strings.rooms, Icons.Outlined.GridView, !activeReview, onRooms)
        JourneySegment(Strings.reviewShort, Icons.AutoMirrored.Outlined.FactCheck, activeReview, onReview)
    }
}

@Composable
private fun RowScope.JourneySegment(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = active,
        onClick = onClick,
        modifier = Modifier.weight(1f).semantics { role = Role.Tab },
        shape = MaterialTheme.shapes.medium,
        color = if (active) colors.secondaryContainer else Color.Transparent,
        contentColor = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
