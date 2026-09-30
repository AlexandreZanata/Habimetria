package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vault.local.LocalSession
import vault.local.LocationClaim
import vault.local.STATUS_DRAFT
import vault.local.ShotCategory
import vault.local.db.LocalEvidence
import vault.local.newLocalId
import vault.local.parseNaCategories
import vault.local.platformLocationTracker
import vault.local.readExifGps
import vault.nav.useSideBySide
import vault.nav.windowClassFor
import vault.rooms.RoomCoverage

fun shotLabel(category: ShotCategory): String = when (category) {
    ShotCategory.OVERVIEW -> Strings.shotOverview
    ShotCategory.WALLS -> Strings.shotWalls
    ShotCategory.OPENINGS -> Strings.shotOpenings
    ShotCategory.FLOOR -> Strings.shotFloor
    ShotCategory.CEILING -> Strings.shotCeiling
}

// LocalRoomScreen: guided capture with per-photo categories, offline.
// Every photo binds a shot category at capture time; coverage counts
// distinct categories, so repeats never fake completion.
@Composable
fun LocalRoomScreen(
    session: LocalSession,
    photoCapture: PhotoCapture,
    inspectionId: String,
    roomId: String,
    roomLabel: String,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tracker = remember { platformLocationTracker() }
    var lastClaim by remember { mutableStateOf<LocationClaim?>(null) }
    var evidences by remember { mutableStateOf<List<LocalEvidence>>(emptyList()) }
    var coverage by remember { mutableStateOf<RoomCoverage?>(null) }
    var na by remember { mutableStateOf(setOf<ShotCategory>()) }
    var selected by remember { mutableStateOf(ShotCategory.OVERVIEW) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                val room = session.repository.roomsFor(inspectionId).firstOrNull { it.id == roomId }
                na = parseNaCategories(room?.naCategories.orEmpty())
                evidences = session.repository.evidencesForRoom(roomId)
                coverage = session.coverage.roomCoverage(roomId)
                error = null
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
        }
    }

    LaunchedEffect(roomId) { refresh() }

    fun capture() {
        busy = true
        error = null
        scope.launch {
            try {
                if (session.repository.getInspection(inspectionId)?.status != STATUS_DRAFT) {
                    error = Strings.sealed
                    return@launch
                }
                val shot = when (val result = photoCapture.captureDetailed()) {
                    is CaptureResult.Failure -> {
                        error = captureErrorString(result.reason)
                        return@launch
                    }
                    is CaptureResult.Photo -> result
                }
                val evidenceId = newLocalId("ev")
                // Location reads the cached fix and returns immediately:
                // the photo never waits for GPS.
                val claim = tracker.lastClaim().also { lastClaim = it }
                val exif = withContext(Dispatchers.Default) { readExifGps(shot.bytes) }
                val stored = withContext(Dispatchers.Default) {
                    session.originals.storeOriginal(evidenceId, shot.bytes)
                }
                withContext(Dispatchers.Default) {
                    session.repository.recordEvidence(
                        inspectionId = inspectionId,
                        roomId = roomId,
                        fileName = "$evidenceId.bin",
                        mime = shot.mime,
                        size = stored.size,
                        sha256 = stored.sha256,
                        shotCategory = selected,
                        evidenceId = evidenceId,
                        location = claim,
                        exif = exif,
                    )
                }
                // Best-effort: a codec failure here never loses the photo;
                // views fall back to the original and retry lazily.
                withContext(Dispatchers.Default) {
                    try {
                        session.derivatives.ensureDerivatives(evidenceId, shot.bytes)
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                error = e.message?.take(160)
            } finally {
                busy = false
                refresh()
            }
        }
    }

    fun toggleNa(category: ShotCategory) {
        scope.launch {
            try {
                val next = if (category in na) na - category else na + category
                session.repository.setRoomNotApplicable(roomId, next)
            } catch (e: Exception) {
                error = e.message?.take(160)
            }
            refresh()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (useSideBySide(windowClassFor(maxWidth.value.toInt()))) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CameraPreview(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { contentDescription = Strings.cameraPreview },
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LocalRoomControls(
                        session, evidences, coverage, na, selected,
                        { selected = it }, busy, error, lastClaim, ::toggleNa, ::capture, onReview, onBack,
                    )
                }
            }
        } else {
            ScreenColumn {
                ScreenTitle(roomLabel)
                CameraPreview(
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                        .semantics { contentDescription = Strings.cameraPreview },
                )
                LocalRoomControls(
                    session, evidences, coverage, na, selected,
                    { selected = it }, busy, error, lastClaim, ::toggleNa, ::capture, onReview, onBack,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LocalRoomControls(
    session: LocalSession,
    evidences: List<LocalEvidence>,
    coverage: RoomCoverage?,
    na: Set<ShotCategory>,
    selected: ShotCategory,
    onSelect: (ShotCategory) -> Unit,
    busy: Boolean,
    error: String?,
    lastClaim: LocationClaim?,
    onToggleNa: (ShotCategory) -> Unit,
    onCapture: () -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    Text("${Strings.captureIn}:")
    ShotCategory.entries.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { category ->
                val done = coverage?.captured?.contains(category.name) == true
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text((if (done) "✓ " else "") + shotLabel(category)) },
                )
            }
        }
    }
    VaultButton(
        text = if (busy) Strings.saving else Strings.capture,
        onClick = onCapture,
        enabled = !busy,
    )
    ErrorText(error)
    lastClaim?.let { claim ->
        Text(
            "${Strings.locationLabel}: " + claimSummary(
                claim.status.name, claim.accuracyM, claim.fixAgeMs, claim.isMocked,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val percent = coverage?.percent() ?: 0
    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
    Text("${Strings.coverage}: $percent%")
    Text("${Strings.missingShots}:")
    val doneSet = coverage?.captured?.mapNotNull { ShotCategory.parse(it) }?.toSet() ?: emptySet()
    ShotCategory.entries.forEach { category ->
        val state = when {
            category in na -> "⊘ ${shotLabel(category)}"
            category in doneSet -> "✓ ${shotLabel(category)}"
            else -> "○ ${shotLabel(category)}"
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (category !in doneSet) {
                TextButton(onClick = { onToggleNa(category) }) {
                    Text(if (category in na) "✓ ${Strings.notApplicable}" else Strings.notApplicable)
                }
            }
        }
    }
    Text("${Strings.evidenceList} (${evidences.size})")
    evidences.forEach { evidence ->
        val thumb = remember(evidence.id) {
            val path = session.derivatives.ensureForDisplay(
                evidence.id, session.originals.originalPath(evidence.id),
            )
            runCatching { decodeThumbnail(path) }.getOrNull()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = "${Strings.photosSuffix}: ${evidence.shotCategory}",
                    modifier = Modifier.size(56.dp),
                )
            }
            Text(
                "#${evidence.sequence} ${evidence.shotCategory ?: "?"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    VaultButton(text = "${Strings.review} →", onClick = onReview)
    VaultButton(text = Strings.back, onClick = onBack)
}
