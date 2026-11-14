package vault.pdf

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import vault.local.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosPlanRenderer : PlanRenderer {
    private val text = IosPdfText()
    override fun measureText(text: String, kind: TextKind, width: Double) = this.text.measureText(text, kind, width)
    override fun lineHeight(kind: TextKind) = text.lineHeight(kind)

    override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean): ByteArray = withContext(Dispatchers.Default) {
        val backing = NSMutableData()
        val bounds = CGRectMake(0.0, 0.0, PDF_PAGE_WIDTH_PT, PDF_PAGE_HEIGHT_PT)
        UIGraphicsBeginPDFContextToData(backing, bounds, null)
        try {
            for ((index, page) in pages.withIndex()) {
                if (isCancelled()) throw RenderCancelledException()
                UIGraphicsBeginPDFPageWithInfo(bounds, null)
                val context = UIGraphicsGetCurrentContext() ?: error("PDF context unavailable")
                for (item in page.items + page.chrome) {
                    if (isCancelled()) throw RenderCancelledException()
                    when (item) {
                        is PageItem.Fill -> {
                            item.tone.uiColor.setFill()
                            CGContextFillRect(context, CGRectMake(item.rect.x, item.rect.y, item.rect.w, item.rect.h))
                        }
                        is PageItem.Path -> {
                            CGContextBeginPath(context)
                            val start = item.points.first()
                            CGContextMoveToPoint(context, start.x, start.y)
                            for (point in item.points.drop(1)) CGContextAddLineToPoint(context, point.x, point.y)
                            val width = item.strokeWidth
                            if (width == null) {
                                item.tone.uiColor.setFill()
                                CGContextFillPath(context)
                            } else {
                                item.tone.uiColor.setStroke()
                                CGContextSetLineWidth(context, width)
                                CGContextStrokePath(context)
                            }
                        }
                        is PageItem.Text -> text.draw(item)
                        is PageItem.Photo -> {
                            val image = iosImageThumbnail(requireNotNull(loadPhoto(item.entry.evidenceId)), PREVIEW_MAX_EDGE)
                            try { drawIosPdfImage(context, image, item.rect) } finally { CGImageRelease(image) }
                            text.drawStamp(item.rect, item.entry.stampLines)
                        }
                        is PageItem.Qr -> {
                            val image = iosPdfQr(item.content)
                            try { drawIosPdfImage(context, image, requireNotNull(item.rect)) } finally { CGImageRelease(image) }
                        }
                        is PageItem.Finding, is PageItem.HashRow -> Unit
                    }
                }
                onProgress(index + 1, pages.size)
            }
        } finally { UIGraphicsEndPDFContext() }
        // The trailer and native writer are finalized before consuming bytes.
        val size = backing.length.toInt()
        require(size > 0) { "empty finalized PDF" }
        requireNotNull(backing.bytes).readBytes(size)
    }
}
