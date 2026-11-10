package vault.ui.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vault.local.LocalSession
import vault.local.ShotCategory
import vault.local.db.LocalEvidence
import vault.ui.Strings
import vault.ui.decodeThumbnail
import vault.ui.shotLabel

// One photo row: tap the thumbnail to view full-screen, Excluir removes
// the photo end to end (row, findings, files) and refreshes coverage.
@Composable
fun EvidencePhotoRow(
    session: LocalSession,
    evidence: LocalEvidence,
    onView: () -> Unit,
    onDelete: () -> Unit,
) {
    val thumb = remember(evidence.id) {
        val path = session.derivatives.ensureForDisplay(
            evidence.id, session.originals.originalPath(evidence.id),
        )
        runCatching { decodeThumbnail(path) }.getOrNull()
    }
    val categoryLabel = ShotCategory.parse(evidence.shotCategory ?: "")?.let { shotLabel(it) } ?: Strings.evidence
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onView),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                Image(bitmap = thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Outlined.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(JourneyStrings.photoNumber(evidence.sequence), style = MaterialTheme.typography.titleSmall)
            Text(categoryLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = JourneyStrings.deletePhoto(evidence.sequence), tint = MaterialTheme.colorScheme.error)
        }
    }
}

// Full-screen viewer: tap outside, the ✕ button or system back closes.
@Composable
fun PhotoViewer(
    session: LocalSession,
    evidence: LocalEvidence,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onClose),
        ) {
            val full = remember(evidence.id) {
                runCatching {
                    decodeThumbnail(session.originals.originalPath(evidence.id), 1600)
                }.getOrNull()
            }
            if (full != null) {
                Image(
                    bitmap = full,
                    contentDescription = "${Strings.photosSuffix}: ${evidence.shotCategory}",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) { Text("✕ ${Strings.close}", color = Color.White) }
        }
    }
}
