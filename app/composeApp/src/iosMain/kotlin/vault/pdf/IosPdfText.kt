package vault.pdf

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreText.*
import platform.Foundation.*
import vault.composeapp.generated.resources.Res
import platform.UIKit.*
import vault.local.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosPdfText : PdfTextMetrics {
    private val fonts by lazy {
        for (weight in PdfFontWeight.entries) {
            val uri = Res.getUri("font/manrope_${weight.name.lowercase()}.ttf")
            val url = requireNotNull(NSURL.URLWithString(uri))
            val reference = CFBridgingRetain(url) as CFURLRef
            try { CTFontManagerRegisterFontsForURL(reference, kCTFontManagerScopeProcess, null) }
            finally { CFRelease(reference) }
        }
        TextKind.entries.associateWith { kind ->
            val weight = when (kind.weight) {
                PdfFontWeight.REGULAR -> "Regular"
                PdfFontWeight.MEDIUM -> "Medium"
                PdfFontWeight.SEMIBOLD -> "SemiBold"
            }
            UIFont.fontWithName("ManropeExtraLight-$weight", kind.size)
                ?: UIFont.systemFontOfSize(kind.size)
        }
    }

    private fun font(kind: TextKind): UIFont = fonts.getValue(kind)

    private fun attributed(text: String, kind: TextKind, tone: PdfTone = kind.tone): NSAttributedString =
        NSAttributedString.create(
            string = text, attributes = mapOf<Any?, Any?>(
                NSFontAttributeName to font(kind), NSForegroundColorAttributeName to tone.uiColor,
            ),
        )

    override fun lineHeight(kind: TextKind): Double = maxOf(super.lineHeight(kind), font(kind).lineHeight)

    override fun measureText(text: String, kind: TextKind, width: Double): List<String> =
        measuredPdfLines(text, width) { textWidth(attributed(it, kind)) }

    fun draw(item: PageItem.Text) {
        val rect = requireNotNull(item.rect)
        val line = attributed(item.text, item.kind, item.tone)
        val origin = when (item.align) {
            PdfAlign.LEFT -> rect.x
            PdfAlign.CENTER -> rect.x + (rect.w - textWidth(line)) / 2
            PdfAlign.RIGHT -> rect.x + rect.w - textWidth(line)
        }
        line.drawAtPoint(CGPointMake(origin, rect.y))
    }
    // Location stamp over the photo (bottom-right, white with shadow),
    // mirroring field geotag-camera proofs. Page-only; photo bytes untouched.
    fun drawStamp(rect: PdfRect, lines: List<String>) {
        val visible = lines.filter { it.isNotBlank() }
        if (visible.isEmpty()) return
        val step = lineHeight(TextKind.SMALL) + 2.0
        var originY = rect.y + rect.h - 4.0 - lineHeight(TextKind.SMALL)
        for (line in visible.asReversed()) {
            val originX = rect.x + rect.w - 4.0 - textWidth(stamped(line))
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
    private fun textWidth(text: NSAttributedString): Double {
        val attr = CFBridgingRetain(text) as CFAttributedStringRef
        try {
            val line = CTLineCreateWithAttributedString(attr) ?: error("text measurement failed")
            try { return CTLineGetTypographicBounds(line, null, null, null) } finally { CFRelease(line) }
        } finally { CFRelease(attr) }
    }
}

internal val PdfTone.uiColor: UIColor get() = UIColor.colorWithRed(
    (rgb shr 16 and 255) / 255.0, (rgb shr 8 and 255) / 255.0, (rgb and 255) / 255.0, 1.0,
)
