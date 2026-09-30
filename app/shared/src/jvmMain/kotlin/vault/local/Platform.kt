package vault.local

import java.time.Instant
import java.util.UUID

actual fun newLocalId(prefix: String): String =
    prefix + "_" + UUID.randomUUID().toString().replace("-", "").take(12)

actual fun deviceNowIso(): String = Instant.now().toString()
