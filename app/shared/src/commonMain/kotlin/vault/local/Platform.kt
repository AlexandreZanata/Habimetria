package vault.local

// Platform-provided stable IDs and device-claim timestamps.
// deviceNowIso is a device claim, never server-certified time.
expect fun newLocalId(prefix: String): String

expect fun deviceNowIso(): String

expect fun deviceUtcOffsetMinutes(): Int
