package vault.pdf

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreText.*
import platform.Foundation.*
import platform.UIKit.*
import vault.local.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosPdfText : PdfTextMetrics {
    private fun font(kind: TextKind): UIFont = if (kind == TextKind.TITLE || kind == TextKind.HEADING)
        UIFont.boldSystemFontOfSize(textSize(kind)) else UIFont.systemFontOfSize(textSize(kind))

    private fun attributed(text: String, kind: TextKind): NSAttributedString = NSAttributedString.create(
        string = text, attributes = mapOf<Any?, Any?>(
            NSFontAttributeName to font(kind), NSForegroundColorAttributeName to when (kind) {
                TextKind.TITLE, TextKind.HEADING -> UIColor.colorWithRed(30.0 / 255, 58.0 / 255, 95.0 / 255, 1.0)
                TextKind.SMALL -> UIColor.darkGrayColor
                TextKind.BODY -> UIColor.blackColor
            },
        ),
    )
    override fun lineHeight(kind: TextKind): Double = maxOf(super.lineHeight(kind), font(kind).lineHeight)
    override fun measureText(text: String, kind: TextKind, width: Double): List<String> = measuredPdfLines(text, width) {
        val attr = CFBridgingRetain(attributed(it, kind)) as CFAttributedStringRef
        try {
            val line = CTLineCreateWithAttributedString(attr) ?: error("text measurement failed")
            try { CTLineGetTypographicBounds(line, null, null, null) } finally { CFRelease(line) }
        } finally { CFRelease(attr) }
    }
    fun draw(item: PageItem.Text) {
        val rect = requireNotNull(item.rect)
        attributed(item.text, item.kind).drawAtPoint(CGPointMake(rect.x, rect.y))
    }
    fun chrome(page: RenderPage, total: Int) {
        attributed("VISTORIA | REGISTRO FOTOGRÁFICO", TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 36.0))
        attributed(page.header, TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 797.0))
        attributed("${page.number} / $total", TextKind.SMALL).drawAtPoint(CGPointMake(495.0, 797.0))
        attributed("Hora e localização informadas pelo dispositivo", TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 814.0))
    }
}
