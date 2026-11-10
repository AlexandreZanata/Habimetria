package vault.pdf

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import vault.local.*

internal class AndroidPdfFonts : PdfTextMetrics {
    private val paints = TextKind.entries.associateWith { kind ->
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = vault.local.textSize(kind).toFloat()
            typeface = Typeface.create(Typeface.SANS_SERIF,
                if (kind == TextKind.TITLE || kind == TextKind.HEADING) Typeface.BOLD else Typeface.NORMAL)
            color = when (kind) {
                TextKind.TITLE, TextKind.HEADING -> Color.rgb(18, 61, 53)
                TextKind.SMALL -> Color.rgb(40, 51, 47)
                TextKind.BODY -> Color.BLACK
            }
        }
    }
    fun paint(kind: TextKind): TextPaint = paints.getValue(kind)
    override fun measureText(text: String, kind: TextKind, width: Double): List<String> =
        measuredPdfLines(text, width) { paint(kind).measureText(it).toDouble() }
    override fun lineHeight(kind: TextKind): Double = maxOf(super.lineHeight(kind), paint(kind).fontSpacing.toDouble())
}
