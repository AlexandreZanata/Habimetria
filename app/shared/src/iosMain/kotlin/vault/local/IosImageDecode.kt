package vault.local

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun iosImageThumbnail(bytes: ByteArray, maxEdge: Int): CGImageRef {
    require(bytes.isNotEmpty())
    return bytes.usePinned { pinned ->
        val provider = CGDataProviderCreateWithData(null, pinned.addressOf(0), bytes.size.convert(), null)
            ?: error("image provider failed")
        try {
            val source = CGImageSourceCreateWithDataProvider(provider, null) ?: error("undecodable photo")
            try {
                val options = NSMutableDictionary().apply {
                    setValue(NSNumber(int = maxEdge), forKey = cfKey(kCGImageSourceThumbnailMaxPixelSize))
                    setValue(NSNumber(bool = true), forKey = cfKey(kCGImageSourceCreateThumbnailFromImageAlways))
                    setValue(NSNumber(bool = true), forKey = cfKey(kCGImageSourceCreateThumbnailWithTransform))
                    setValue(NSNumber(bool = true), forKey = cfKey(kCGImageSourceShouldCacheImmediately))
                }
                val cfOptions = CFBridgingRetain(options)
                try {
                    CGImageSourceCreateThumbnailAtIndex(source, 0u, cfOptions?.reinterpret())
                        ?: error("undecodable photo thumbnail")
                } finally { cfOptions?.let { CFRelease(it) } }
            } finally { CFRelease(source) }
        } finally { CGDataProviderRelease(provider) }
    }
}
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun cfKey(key: CFStringRef?): String = CFBridgingRelease(CFRetain(key)) as? String
    ?: error("missing ImageIO option key")
