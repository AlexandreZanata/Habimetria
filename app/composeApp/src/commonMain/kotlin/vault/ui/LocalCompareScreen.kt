package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.CompareTag
import vault.local.LocalSession
import vault.local.PositionPair
import vault.local.db.LocalEvidence

// Entry-vs-exit per room position: side-by-side or drag slider, then a
// user-confirmed tag. Tags describe visual state only, never liability,
// and nothing here is automated.
@Composable
fun LocalCompareScreen(
    session: LocalSession,
    exitInspectionId: String,
    onDiff: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pairs by remember { mutableStateOf<List<PositionPair>>(emptyList()) }
    var tenancyId by remember { mutableStateOf<String?>(null) }
    var sideBySide by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                tenancyId = session.repository.getInspection(exitInspectionId)?.tenancyId
                pairs = session.compare.pairsForExit(exitInspectionId)
                error = null
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    LaunchedEffect(exitInspectionId) { refresh() }

    ScreenColumn {
        ScreenTitle(Strings.compareTitle)
        ErrorText(error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { sideBySide = true }) { Text(Strings.compareSideBySide) }
            TextButton(onClick = { sideBySide = false }) { Text(Strings.compareSlider) }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val grouped = pairs.groupBy { it.roomLabel }
            grouped.forEach { (label, roomPairs) ->
                item(key = "header-$label") {
                    Text("$label", style = MaterialTheme.typography.titleMedium)
                }
                items(roomPairs, key = { "${it.exitRoomId}-${it.category}" }) { pair ->
                    ComparePairBlock(
                        session = session,
                        tenancyId = tenancyId,
                        pair = pair,
                        sideBySide = sideBySide,
                        onTagged = { refresh() },
                        onError = { error = it },
                    )
                }
            }
        }
        VaultButton(text = "${Strings.diffTitle} →", onClick = onDiff)
        VaultButton(text = Strings.back, onClick = onBack)
    }
}

@Composable
private fun ComparePairBlock(
    session: LocalSession,
    tenancyId: String?,
    pair: PositionPair,
    sideBySide: Boolean,
    onTagged: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val entryBitmap = remember(pair.entryEvidence?.id) {
        pair.entryEvidence?.let { decodeThumb(session, it) }
    }
    val exitBitmap = remember(pair.exitEvidence?.id) {
        pair.exitEvidence?.let { decodeThumb(session, it) }
    }
    Text(
        "${shotLabel(pair.category)} · ${Strings.compareEntry}: ${pair.entryEvidence?.let { "#${it.sequence}" } ?: Strings.compareNoPhoto} · " +
            "${Strings.compareExit}: ${pair.exitEvidence?.let { "#${it.sequence}" } ?: Strings.compareNoPhoto}",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (sideBySide) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompareThumb(entryBitmap, Strings.compareEntry, Modifier.weight(1f))
            CompareThumb(exitBitmap, Strings.compareExit, Modifier.weight(1f))
        }
    } else {
        CompareSlider(entry = entryBitmap, exit = exitBitmap)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tagChip(CompareTag.NO_CHANGE, Strings.tagNoChange, pair.tag) { tag ->
            saveTag(session, scope, tenancyId, pair, tag, onTagged, onError)
        }
        tagChip(CompareTag.CHANGED, Strings.tagChanged, pair.tag) { tag ->
            saveTag(session, scope, tenancyId, pair, tag, onTagged, onError)
        }
        tagChip(CompareTag.WEAR, Strings.tagWear, pair.tag) { tag ->
            saveTag(session, scope, tenancyId, pair, tag, onTagged, onError)
        }
    }
}

@Composable
private fun tagChip(
    tag: CompareTag,
    label: String,
    current: CompareTag?,
    onSelect: (CompareTag) -> Unit,
) {
    FilterChip(
        selected = current == tag,
        onClick = { onSelect(tag) },
        label = { Text((if (current == tag) "✓ " else "") + label) },
    )
}

private fun saveTag(
    session: LocalSession,
    scope: kotlinx.coroutines.CoroutineScope,
    tenancyId: String?,
    pair: PositionPair,
    tag: CompareTag,
    onTagged: () -> Unit,
    onError: (String) -> Unit,
) {
    val tenancy = tenancyId ?: return
    scope.launch {
        try {
            session.compare.setTag(
                tenancyId = tenancy,
                entryRoomId = pair.entryRoomId,
                exitRoomId = pair.exitRoomId,
                category = pair.category,
                tag = tag,
                entryEvidenceId = pair.entryEvidence?.id,
                exitEvidenceId = pair.exitEvidence?.id,
            )
            onTagged()
        } catch (e: Exception) {
            onError(e.message?.take(160) ?: "")
        }
    }
}

private fun decodeThumb(session: LocalSession, evidence: LocalEvidence): ImageBitmap? {
    val path = session.derivatives.ensureForDisplay(
        evidence.id, session.originals.originalPath(evidence.id),
    )
    return runCatching { decodeThumbnail(path) }.getOrNull()
}

@Composable
private fun CompareThumb(bitmap: ImageBitmap?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (bitmap != null) {
            Image(bitmap, label, Modifier.fillMaxWidth())
        } else {
            Text(Strings.compareNoPhoto, style = MaterialTheme.typography.bodyMedium)
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
