package vault.local

import java.time.Instant

actual fun certificateDeviceValidity(validFromIso: String, validUntilIso: String): Pair<Boolean, Boolean> {
    return try {
        val now = Instant.now()
        val expired = Instant.parse(validUntilIso).isBefore(now)
        val notYet = Instant.parse(validFromIso).isAfter(now)
        expired to notYet
    } catch (_: Exception) {
        false to false
    }
}
