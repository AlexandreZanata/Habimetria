package vault.pdf

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGFloatVar
import platform.CoreText.CTLineCreateWithAttributedString
import platform.CoreText.CTLineGetTypographicBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFAttributedStringRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSAttributedString
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.setValue
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithDataProvider
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.CoreImage.CIFilter
import platform.CoreImage.CIContext
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsBeginPDFPageWithInfo
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.drawAtPoint
import vault.local.PDF_BODY_SIZE
import vault.local.PDF_HEADING_SIZE
import vault.local.PDF_MARGIN_BOTTOM_PT
import vault.local.PDF_MARGIN_LEFT_PT
import vault.local.PDF_MARGIN_RIGHT_PT
import vault.local.PDF_MARGIN_TOP_PT
import vault.local.PDF_PAGE_HEIGHT_PT
import vault.local.PDF_PAGE_WIDTH_PT
import vault.local.PDF_SMALL_SIZE
import vault.local.PDF_TITLE_SIZE
import vault.local.PageItem
import vault.local.PlanRenderer
import vault.local.RenderCancelledException
import vault.local.RenderPage
import vault.local.TextKind

// iOS PlanRenderer: same shared items as Android, drawn with UIKit/Core
// Graphics. Photos decode through the thumbnail API (bounded pixels),
// one at a time with immediate release; continuation pages keep the
// no-drop invariant on font-metric drift.
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosPlanRenderer : PlanRenderer {

    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray = withContext(Dispatchers.Default) {
        val backing = NSMutableData()
        UIGraphicsBeginPDFContextToData(
            data = backing,
            bounds = CGRectMake(0.0, 0.0, PDF_PAGE_WIDTH_PT, PDF_PAGE_HEIGHT_PT),
            documentInfo = null,
        )
        try {
            pages.forEachIndexed { index, page ->
                if (isCancelled()) throw RenderCancelledException()
                val ctx = PageContext(page.header, index + 1, pages.size)
                for (item in page.items) {
                    if (isCancelled()) throw RenderCancelledException()
                    when (item) {
                        is PageItem.Photo -> ctx.drawPhoto(item, loadPhoto(item.entry.evidenceId))
                        is PageItem.Text -> ctx.drawTextBlock(item.text, fontFor(item.kind), 8.0)
                        is PageItem.Finding -> ctx.drawTextBlock(
                            "• ${item.entry.preset} — ${item.entry.note ?: ""} (${item.entry.evidenceRef})".take(400),
                            bodyFont(), 4.0,
                        )
                        is PageItem.HashRow -> {
                            ctx.drawTextBlock(item.label, smallFont(), 0.0)
                            ctx.drawTextBlock(item.hex, smallFont(), 6.0)
                        }
                        is PageItem.Qr -> ctx.drawQr(item.content)
                    }
                }
                onProgress(index + 1, pages.size)
            }
            backing.toByteArray()
        } finally {
            UIGraphicsEndPDFContext()
        }
    }

    private fun titleFont(): UIFont = UIFont.boldSystemFontOfSize(PDF_TITLE_SIZE)
    private fun headingFont(): UIFont = UIFont.boldSystemFontOfSize(PDF_HEADING_SIZE)
    private fun bodyFont(): UIFont = UIFont.systemFontOfSize(PDF_BODY_SIZE)
    private fun smallFont(): UIFont = UIFont.systemFontOfSize(PDF_SMALL_SIZE)

    private fun fontFor(kind: TextKind): UIFont = when (kind) {
        TextKind.TITLE -> titleFont()
        TextKind.HEADING -> headingFont()
        TextKind.BODY -> bodyFont()
        TextKind.SMALL -> smallFont()
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private inner class PageContext(
        private val header: String,
        private val number: Int,
        private val total: Int,
    ) {
        private var ctx: CGContextRef? = null
        private var cursorY = PDF_MARGIN_TOP_PT
        private val bottom = PDF_PAGE_HEIGHT_PT - PDF_MARGIN_BOTTOM_PT

        init {
            open(continued = false)
        }

        private fun open(continued: Boolean) {
            UIGraphicsBeginPDFPageWithInfo(
                bounds = CGRectMake(0.0, 0.0, PDF_PAGE_WIDTH_PT, PDF_PAGE_HEIGHT_PT),
                pageInfo = null,
            )
            ctx = UIGraphicsGetCurrentContext()
            val suffix = if (continued) " (cont.)" else ""
            drawLine(header.take(48), PDF_MARGIN_LEFT_PT, 36.0, smallFont())
            drawLine(
                "p. $number/$total",
                PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT - 60.0,
                PDF_PAGE_HEIGHT_PT - 24.0,
                smallFont(),
            )
            cursorY = PDF_MARGIN_TOP_PT
        }

        private fun ensure(space: Double) {
            if (cursorY + space <= bottom) return
            open(continued = true)
        }

        fun drawTextBlock(text: String, font: UIFont, reserved: Double) {
            val lines = wrapLines(text, font, contentWidth())
            ensure(lines.size * font.lineHeight + reserved)
            var y = cursorY
            for (line in lines) {
                drawLine(line, PDF_MARGIN_LEFT_PT, y, font)
                y += font.lineHeight
            }
            cursorY = y + reserved
        }

        fun drawPhoto(item: PageItem.Photo, bytes: ByteArray?) {
            val rect = item.rect
            if (cursorY > rect.y + 4) cursorY = rect.y
            ensure(rect.h + 30.0)
            val image = bytes?.let { decodeBounded(it, 1200) }
            if (image != null) {
                try {
                    drawImageFlipped(image, rect.x, rect.y, rect.w, rect.h)
                } finally {
                    CGImageRelease(image)
                }
            } else {
                drawLine("[foto indisponível]", rect.x, rect.y + 12.0, smallFont())
            }
            drawLine(item.entry.caption.take(90), rect.x, rect.y + rect.h + 11.0, smallFont())
            item.entry.claimLine?.let {
                drawLine(it.take(110), rect.x, rect.y + rect.h + 22.0, smallFont())
            }
            cursorY = rect.y + rect.h + 30.0
        }

        fun drawQr(content: String) {
            val side = 140.0
            ensure(side + 8.0)
            val image = qrImage(content, 280) ?: return
            try {
                drawImageFlipped(image, PDF_MARGIN_LEFT_PT, cursorY, side, side)
            } finally {
                CGImageRelease(image)
            }
            cursorY += side + 8.0
        }

        private fun contentWidth(): Double =
            PDF_PAGE_WIDTH_PT - PDF_MARGIN_LEFT_PT - PDF_MARGIN_RIGHT_PT

        private fun drawLine(text: String, x: Double, y: Double, font: UIFont) {
            val g = ctx ?: return
            CGContextSaveGState(g)
            try {
                // UIKit PDF contexts use top-left origin: text draws upright.
                attributed(text, font).drawAtPoint(CGPointMake(x, y))
            } finally {
                CGContextRestoreGState(g)
            }
        }

        private fun attributed(text: String, font: UIFont): NSAttributedString =
            NSAttributedString.create(
                string = text,
                attributes = mapOf<Any?, Any?>(
                    NSFontAttributeName to font,
                    NSForegroundColorAttributeName to UIColor.colorWithRed(0.0, green = 0.0, blue = 0.0, alpha = 1.0),
                ),
            )

        private fun wrapLines(text: String, font: UIFont, maxWidth: Double): List<String> {
            fun widthOf(s: String): Double = memScoped {
                val cfAttr = CFBridgingRetain(attributed(s, font)) as CFAttributedStringRef
                try {
                    val line = CTLineCreateWithAttributedString(cfAttr) ?: return 0.0
                    try {
                        val ascent = alloc<CGFloatVar>()
                        val descent = alloc<CGFloatVar>()
                        val leading = alloc<CGFloatVar>()
                        CTLineGetTypographicBounds(line, ascent.ptr, descent.ptr, leading.ptr)
                    } finally {
                        platform.CoreFoundation.CFRelease(line)
                    }
                } finally {
                    platform.CoreFoundation.CFRelease(cfAttr)
                }
            }
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (widthOf(candidate) <= maxWidth || current.isEmpty()) {
                    current = candidate
                } else {
                    lines.add(current)
                    current = word
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            return lines.ifEmpty { listOf("") }
        }

        // CGImages draw upside-down in the flipped UIKit context; the
        // local flip cancels it inside the destination rect only.
        private fun drawImageFlipped(image: CGImageRef?, x: Double, y: Double, w: Double, h: Double) {
            val g = ctx ?: return
            val img = image ?: return
            CGContextSaveGState(g)
            try {
                CGContextTranslateCTM(g, x, y + h)
                CGContextScaleCTM(g, 1.0, -1.0)
                platform.CoreGraphics.CGContextDrawImage(g, CGRectMake(0.0, 0.0, w, h), img)
            } finally {
                CGContextRestoreGState(g)
            }
        }
    }

    private fun decodeBounded(bytes: ByteArray, maxEdge: Int): CGImageRef? {
        if (bytes.isEmpty()) return null
        return bytes.usePinned { pinned ->
            val provider = CGDataProviderCreateWithData(
                null,
                pinned.addressOf(0),
                bytes.size.convert(),
                null,
            ) ?: return@usePinned null
            try {
                val source = platform.ImageIO.CGImageSourceCreateWithDataProvider(provider, null)
                    ?: return@usePinned null
                try {
                    thumbnail(source, maxEdge)
                        ?: platform.ImageIO.CGImageSourceCreateImageAtIndex(source, 0u, null)
                } finally {
                    CFRelease(source)
                }
            } finally {
                CGDataProviderRelease(provider)
            }
        }
    }

    private fun thumbnail(
        source: platform.ImageIO.CGImageSourceRef,
        maxPixelSize: Int,
    ): CGImageRef? {
        val keyMaxSize = CFBridgingRelease(CFRetain(platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize)) as? String
            ?: return null
        val keyAlways = CFBridgingRelease(CFRetain(platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways)) as? String
            ?: return null
        val options = NSMutableDictionary().apply {
            setValue(NSNumber(int = maxPixelSize), forKey = keyMaxSize)
            setValue(NSNumber(bool = true), forKey = keyAlways)
        }
        val cfOptions = CFBridgingRetain(options)
        try {
            return platform.ImageIO.CGImageSourceCreateThumbnailAtIndex(source, 0u, cfOptions?.reinterpret())
        } finally {
            cfOptions?.let { CFRelease(it) }
        }
    }

    private fun qrImage(content: String, pixels: Int): CGImageRef? {
        val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return null
        filter.setValue(content.encodeToNSData(), forKey = "inputMessage")
        filter.setValue("M", forKey = "inputCorrectionLevel")
        val output = filter.outputImage ?: return null
        val scale = pixels / output.extent.useContents { size.width }
        val scaled = output.imageByApplyingTransform(
            platform.CoreGraphics.CGAffineTransformMakeScale(scale, scale),
        )
        val context = CIContext.contextWithOptions(null)
        return context.createCGImage(scaled, fromRect = scaled.extent)
    }

    private fun String.encodeToNSData(): NSData =
        NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("UTF-8 encode failed")

    private fun ByteArray.toCFData(): platform.CoreFoundation.CFDataRef? {
        if (isEmpty()) return null
        return usePinned {
            platform.CoreFoundation.CFDataCreate(
                platform.CoreFoundation.kCFAllocatorDefault,
                it.addressOf(0).reinterpret(),
                size.convert(),
            )
        }
    }

    private fun NSMutableData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return ByteArray(0)
        val pointer = this.bytes ?: return ByteArray(0)
        return pointer.readBytes(length)
    }
}
