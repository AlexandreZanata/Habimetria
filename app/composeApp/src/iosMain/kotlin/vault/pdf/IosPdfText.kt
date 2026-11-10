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
    // Location stamp over the photo (bottom-right, white with shadow),
    // mirroring field geotag-camera proofs. Page-only; photo bytes untouched.
    fun drawStamp(rect: PdfRect, lines: List<String>) {
        val visible = lines.filter { it.isNotBlank() }
        if (visible.isEmpty()) return
        val step = lineHeight(TextKind.SMALL) + 2.0
        var originY = rect.y + rect.h - 4.0 - lineHeight(TextKind.SMALL)
        for (line in visible.asReversed()) {
            val originX = rect.x + rect.w - 4.0 - stampWidth(line)
            stamped(line).drawAtPoint(CGPointMake(originX, originY))
            originY -= step
        }
    }
    private fun stamped(text: String): NSAttributedString {
        val shadow = NSShadow().apply {
            setShadowOffset(CGSizeMake(1.0, 1.0))
            setShadowBlurRadius(2.5)
            setShadowColor(UIColor.blackColor)
        }
        return NSAttributedString.create(
            string = text, attributes = mapOf<Any?, Any?>(
                NSFontAttributeName to font(TextKind.SMALL),
                NSForegroundColorAttributeName to UIColor.whiteColor,
                NSShadowAttributeName to shadow,
            ),
        )
    }
    private fun stampWidth(text: String): Double {
        val attr = CFBridgingRetain(stamped(text)) as CFAttributedStringRef
        try {
            val line = CTLineCreateWithAttributedString(attr) ?: error("text measurement failed")
            try { return CTLineGetTypographicBounds(line, null, null, null) } finally { CFRelease(line) }
        } finally { CFRelease(attr) }
    }
    fun chrome(page: RenderPage, total: Int) {
        attributed("VISTORIA | REGISTRO FOTOGRÁFICO", TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 36.0))
        attributed(page.header, TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 797.0))
        attributed("${page.number} / $total", TextKind.SMALL).drawAtPoint(CGPointMake(495.0, 797.0))
        attributed("Hora e localização informadas pelo dispositivo", TextKind.SMALL).drawAtPoint(CGPointMake(PDF_MARGIN_LEFT_PT, 814.0))
    }
}
