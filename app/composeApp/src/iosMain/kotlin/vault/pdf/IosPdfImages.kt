package vault.pdf

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.Foundation.*
import platform.ImageIO.*
import vault.local.*

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
