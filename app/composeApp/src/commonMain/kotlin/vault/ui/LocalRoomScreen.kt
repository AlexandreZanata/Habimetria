package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vault.local.LocalSession
import vault.local.LocationClaim
import vault.local.STATUS_DRAFT
import vault.local.ShotCategory
import vault.local.db.LocalEvidence
import vault.local.db.LocalRoom
import vault.local.newLocalId
import vault.local.parseNaCategories
import vault.local.platformLocationTracker
import vault.local.readExifGps
import vault.rooms.RoomCoverage
import vault.ui.journey.JourneyShortcuts
import vault.ui.journey.PhotoViewer
import vault.ui.journey.RoomCaptureControls
import vault.ui.journey.RoomCaptureSkeleton

// LocalRoomScreen: guided capture with per-photo categories, offline.
// Every photo binds a shot category at capture time; coverage counts
// distinct categories, so repeats never fake completion. The selected
// category survives rotation; committed photos live in durable storage.
@Composable
fun LocalRoomScreen(
    session: LocalSession,
    photoCapture: PhotoCapture,
    inspectionId: String,
    roomId: String,
    roomLabel: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onSealed: () -> Unit,
    onReview: () -> Unit,
    onRoomsShortcut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tracker = remember { platformLocationTracker() }
    var lastClaim by remember { mutableStateOf<LocationClaim?>(null) }
    var evidences by remember { mutableStateOf<List<LocalEvidence>>(emptyList()) }
    var nextRoom by remember { mutableStateOf<LocalRoom?>(null) }
    var coverage by remember { mutableStateOf<RoomCoverage?>(null) }
    var na by remember { mutableStateOf(setOf<ShotCategory>()) }
    var selectedName by rememberSaveable { mutableStateOf(ShotCategory.OVERVIEW.name) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<LocalEvidence?>(null) }
    var sealedRedirected by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    val selected = ShotCategory.parse(selectedName) ?: ShotCategory.OVERVIEW

    fun refresh() {
        scope.launch {
            try {
                val inspection = session.repository.getInspection(inspectionId)
                if (inspection != null && inspection.status != STATUS_DRAFT && !sealedRedirected) {
                    sealedRedirected = true
                    onSealed()
                    return@launch
                }
                val allRooms = session.repository.roomsFor(inspectionId)
                val room = allRooms.firstOrNull { it.id == roomId }
                na = parseNaCategories(room?.naCategories.orEmpty())
                val idx = allRooms.indexOfFirst { it.id == roomId }
                nextRoom = if (idx >= 0 && idx + 1 < allRooms.size) allRooms[idx + 1] else null
                evidences = session.repository.evidencesForRoom(roomId)
                coverage = session.coverage.roomCoverage(roomId)
                error = null
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                loaded = true
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
                        deviceUtcOffsetMinutes = vault.local.deviceUtcOffsetMinutes(),
                        exifOrientation = vault.local.jpegExifOrientation(shot.bytes),
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
                error = userErrorMessage(e)
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
                error = userErrorMessage(e)
            }
            refresh()
        }
    }

    fun removeEvidence(evidenceId: String) {
        scope.launch {
            try {
                session.deleteEvidence(evidenceId)
            } catch (e: Exception) {
                error = userErrorMessage(e)
            }
            refresh()
        }
    }

    // No embedded preview: the screen is big buttons and state. Tapping
    // Fotografar opens the camera full-screen; shots return with the
    // selected category bound. Everything scrolls so no action hides
    // below the fold.
    ScreenColumn {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JourneyShortcuts(onRooms = onRoomsShortcut, onReview = onReview)
            val showLoading = rememberLoadingVisible(!loaded)
            if (showLoading) {
                LoadingSkeleton { RoomCaptureSkeleton() }
            } else if (!loaded) {
                // Grace period: data is on its way, paint nothing yet.
            } else {
                RoomCaptureControls(
                    session, evidences, nextRoom, coverage, na, selected,
                    { selectedName = it.name }, busy, error, lastClaim, ::toggleNa, ::capture,
                    ::removeEvidence, { viewing = it }, onOpenRoom, onReview,
                )
            }
        }
    }

    viewing?.let { evidence ->
        PhotoViewer(
            session = session,
            evidence = evidence,
            onClose = { viewing = null },
        )
    }
}
