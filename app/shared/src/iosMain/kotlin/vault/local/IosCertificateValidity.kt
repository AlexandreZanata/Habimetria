package vault.local

// iOS device-clock validity needs manual validation on a supported device
// (iOS checking excluded). Return unknown-valid so unavailable checks are
// never presented as expired or as approved.
actual fun certificateDeviceValidity(validFromIso: String, validUntilIso: String): Pair<Boolean, Boolean> =
    false to false
