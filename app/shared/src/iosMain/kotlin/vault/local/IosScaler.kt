package vault.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

actual fun platformScaler(): ImageScaler = IosScaler()

class IosScaler : ImageScaler {
    @OptIn(ExperimentalForeignApi::class)
    override fun scaleJpeg(bytes: ByteArray, maxEdge: Int): ByteArray {
        if (bytes.isEmpty()) throw LocalStoreException("empty image")
        val data = bytes.toNSData()
        val image = UIImage(data) ?: throw LocalStoreException("undecodable image")
        val w = image.size.useContents { width }
        val h = image.size.useContents { height }
        val longest = maxOf(w, h)
        require(maxEdge > 0)
        val scale = minOf(1.0, maxEdge / longest)
        val target = CGSizeMake(w * scale, h * scale)
        UIGraphicsBeginImageContextWithOptions(target, false, 1.0)
        try {
            val tw = target.useContents { width }
            val th = target.useContents { height }
            image.drawInRect(CGRectMake(0.0, 0.0, tw, th))
            val scaled = UIGraphicsGetImageFromCurrentImageContext()
                ?: throw LocalStoreException("render failed")
            // UIImage drawing applies orientation; original bytes untouched.
            val jpeg: NSData = UIImageJPEGRepresentation(scaled, 0.8)
                ?: throw LocalStoreException("encode failed")
            return jpeg.toByteArray()
        } finally {
            UIGraphicsEndImageContext()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = size.convert())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val out = ByteArray(size)
    memcpy(out.refTo(0), bytes, length)
    return out
}
