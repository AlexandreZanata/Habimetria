package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFCopyDescription
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth

actual fun imageDimensions(bytes: ByteArray): ImageDims? {
    if (bytes.isEmpty()) return null
    return readDims(bytes)
}

@OptIn(ExperimentalForeignApi::class)
private fun readDims(bytes: ByteArray): ImageDims? = memScoped {
    val data: CFDataRef = bytes.usePinned {
        CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), bytes.size.convert())
    } ?: return null
    val source = CGImageSourceCreateWithData(data, null) ?: return null
    val props = CGImageSourceCopyPropertiesAtIndex(source, 0u, null) as CFDictionaryRef?
        ?: return null
    val width = numberProp(props, kCGImagePropertyPixelWidth) ?: return null
    val height = numberProp(props, kCGImagePropertyPixelHeight) ?: return null
    if (width <= 0 || height <= 0) return null
    ImageDims(width.toInt(), height.toInt())
}

@OptIn(ExperimentalForeignApi::class)
private fun numberProp(props: CFDictionaryRef?, key: CFStringRef?): Double? {
    val raw = CFDictionaryGetValue(props, key) as CFNumberRef?
        ?: return null
    val description = CFCopyDescription(raw) ?: return null
    try {
        return description.toString().toDoubleOrNull()
    } finally {
        CFRelease(description)
    }
}

actual fun formatSealedAtDevice(iso: String): String {
    val parsed = platform.Foundation.NSISO8601DateFormatter().apply {
        formatOptions =
            platform.Foundation.NSISO8601DateFormatWithInternetDateTime or
            platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
    }.dateFromString(iso) ?: return iso
    val formatter = platform.Foundation.NSDateFormatter().apply {
        locale = platform.Foundation.NSLocale(localeIdentifier = "pt_BR")
        // System timezone by default: the seal reads in device-local time.
        dateFormat = "dd/MM/yyyy 'às' HH:mm"
    }
    return formatter.stringFromDate(parsed) ?: iso
}
