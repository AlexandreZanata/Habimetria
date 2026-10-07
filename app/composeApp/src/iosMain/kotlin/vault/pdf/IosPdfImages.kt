package vault.pdf

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.Foundation.*
import platform.ImageIO.*
import vault.local.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun iosPdfImage(bytes: ByteArray): CGImageRef {
    require(bytes.isNotEmpty())
    return bytes.usePinned { pinned ->
        val provider = CGDataProviderCreateWithData(null, pinned.addressOf(0), bytes.size.convert(), null)
            ?: error("image provider failed")
        try {
            val source = CGImageSourceCreateWithDataProvider(provider, null) ?: error("undecodable photo")
            try {
                val options = NSMutableDictionary().apply {
                    setValue(NSNumber(int = PREVIEW_MAX_EDGE), forKey = cfKey(kCGImageSourceThumbnailMaxPixelSize))
                    setValue(NSNumber(bool = true), forKey = cfKey(kCGImageSourceCreateThumbnailFromImageAlways))
                    setValue(NSNumber(bool = true), forKey = cfKey(kCGImageSourceCreateThumbnailWithTransform))
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun iosPdfQr(content: String): CGImageRef {
    val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: error("QR filter unavailable")
    val data = content.encodeToByteArray().usePinned { NSData.create(bytes = it.addressOf(0), length = content.encodeToByteArray().size.convert()) }
    filter.setValue(data, forKey = "inputMessage")
    filter.setValue("M", forKey = "inputCorrectionLevel")
    val output = filter.outputImage ?: error("QR generation failed")
    val scale = 400 / output.extent.useContents { size.width }
    val scaled = output.imageByApplyingTransform(CGAffineTransformMakeScale(scale, scale))
    return CIContext().createCGImage(scaled, scaled.extent) ?: error("QR rendering failed")
}

@OptIn(ExperimentalForeignApi::class)
internal fun drawIosPdfImage(context: CGContextRef, image: CGImageRef, rect: PdfRect) {
    CGContextSaveGState(context)
    try {
        CGContextTranslateCTM(context, rect.x, rect.y + rect.h)
        CGContextScaleCTM(context, 1.0, -1.0)
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, rect.w, rect.h), image)
    } finally { CGContextRestoreGState(context) }
}
