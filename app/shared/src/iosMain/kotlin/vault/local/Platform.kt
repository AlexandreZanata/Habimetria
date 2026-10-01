package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
actual fun newLocalId(prefix: String): String =
    prefix + "_" + NSUUID.UUID().UUIDString.replace("-", "").take(12)

@OptIn(ExperimentalForeignApi::class)
actual fun deviceNowIso(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    formatter.timeZone = requireNotNull(NSTimeZone.timeZoneWithName("UTC")) { "UTC time zone missing" }
    return formatter.stringFromDate(NSDate())
}
