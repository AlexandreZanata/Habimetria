package vault.nav

import vault.local.LocalSession
import vault.local.STATUS_SEALED

// Session-backed facts for trailFor, shared by every startup entry.
// Bounded point reads only (no list scans). Archived properties stay
// valid targets so archived history restores; deleted properties and
// missing inspections fall back to list roots; SEALED never restores
// an edit route. Unrelated progress passes through untouched.
suspend fun validatedTrail(session: LocalSession): List<Route> {
    val progress = session.navigation.load()
    val inspectionId = progress.inspectionId?.takeIf { it.isNotBlank() }
    val inspection = inspectionId?.let { session.repository.getInspection(it) }
    val roomId = progress.roomId?.takeIf { it.isNotBlank() }
    val roomExists = if (inspectionId == null || roomId == null) {
        false
    } else {
        session.repository.roomsFor(inspectionId).any { it.id == roomId }
    }
    val propertyId = progress.propertyId?.takeIf { it.isNotBlank() }
    val propertyExists = if (propertyId == null) {
        false
    } else {
        session.repository.getProperty(propertyId) != null
    }
    return trailFor(
        progress,
        RestoreFacts(
            inspectionExists = inspectionId == null || inspection != null,
            inspectionSealed = inspection?.status == STATUS_SEALED,
            roomExists = roomExists,
            propertyExists = propertyExists,
        ),
    )
}
