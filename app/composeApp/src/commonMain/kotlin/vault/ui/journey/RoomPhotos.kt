package vault.ui.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vault.local.LocalSession
import vault.local.db.LocalEvidence
import vault.ui.Strings
import vault.ui.decodeThumbnail

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = "${Strings.photosSuffix}: ${evidence.shotCategory}",
                modifier = Modifier.size(56.dp).clickable(onClick = onView),
            )
        }
        Text(
            "#${evidence.sequence} ${evidence.shotCategory ?: "?"}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).clickable(onClick = onView),
        )
        TextButton(onClick = onDelete) { Text(Strings.delete) }
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
