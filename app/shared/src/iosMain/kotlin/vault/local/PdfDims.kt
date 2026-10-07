package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.alloc
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
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
import platform.ImageIO.kCGImagePropertyOrientation

actual fun imageDimensions(bytes: ByteArray): ImageDims? {
    if (bytes.isEmpty()) return null
    return readDims(bytes)
}

@OptIn(ExperimentalForeignApi::class)
private fun readDims(bytes: ByteArray): ImageDims? = memScoped {
    val data = bytes.usePinned {
        CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), bytes.size.convert())
    } ?: return null
    try {
        val source = CGImageSourceCreateWithData(data, null) ?: return null
        try {
            val props = CGImageSourceCopyPropertiesAtIndex(source, 0u, null) ?: return null
            try {
                val width = numberProp(props, kCGImagePropertyPixelWidth) ?: return null
                val height = numberProp(props, kCGImagePropertyPixelHeight) ?: return null
                if (width <= 0 || height <= 0) return null
                orientedDimensions(width.toInt(), height.toInt(), numberProp(props, kCGImagePropertyOrientation)?.toInt() ?: 1)
            } finally { CFRelease(props) }
        } finally { CFRelease(source) }
    } finally { CFRelease(data) }
}

@OptIn(ExperimentalForeignApi::class)
private fun numberProp(props: CFDictionaryRef?, key: CFStringRef?): Double? = memScoped {
    val raw = CFDictionaryGetValue(props, key) as CFNumberRef? ?: return null
    val value = alloc<DoubleVar>()
    if (platform.CoreFoundation.CFNumberGetValue(raw, platform.CoreFoundation.kCFNumberDoubleType, value.ptr)) value.value else null
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
