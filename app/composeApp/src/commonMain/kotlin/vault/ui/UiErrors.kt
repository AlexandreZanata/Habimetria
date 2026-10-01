package vault.ui

import vault.local.LocalValidationException
import vault.local.SealedModificationException

// userErrorMessage is the single chokepoint between exceptions and the
// screen: technical English and raw ids never reach the user. Unknown
// failures degrade to one generic pt-BR line instead of a stack trace.
fun userErrorMessage(e: Throwable): String {
    if (e is SealedModificationException) return Strings.sealedLocked
    if (e is LocalValidationException) {
        val raw = e.message.orEmpty()
        if ("is SEALED" in raw) return Strings.sealedLocked
        return when {
            raw.startsWith("address too short") -> Strings.addressTooShort
            raw.startsWith("street too short") -> Strings.streetTooShort
            raw.startsWith("number required") -> Strings.numberRequired
            raw.startsWith("neighborhood too short") -> Strings.neighborhoodTooShort
            raw.startsWith("room label required") -> Strings.roomNameRequired
            raw.startsWith("label over") -> Strings.roomNameTooLong
            raw.startsWith("room has") -> Strings.roomHasPhotos
            raw.startsWith("evidence not found") -> Strings.photoMissing
            raw.startsWith("evidence belongs") -> Strings.actionFailed
            raw.startsWith("sealed entry required") -> Strings.entrySealFirst
            raw.startsWith("entry must be sealed") -> Strings.entrySealFirst
            raw.startsWith("exit already exists") -> Strings.exitAlreadyExists
            raw.startsWith("property archived") -> Strings.archivedBlockNew
            raw.endsWith("not found") -> Strings.recordMissing
            else -> Strings.actionFailed
        }
    }
    return Strings.actionFailed
}
