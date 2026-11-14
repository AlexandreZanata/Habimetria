package vault.pdf

import android.graphics.Typeface
import android.text.TextPaint
import vault.api.AppContext
import vault.composeapp.generated.resources.Res
import vault.local.*

internal class AndroidPdfFonts : PdfTextMetrics {
    private fun face(weight: PdfFontWeight): Typeface {
        val path = Res.getUri("font/manrope_${weight.name.lowercase()}.ttf").removePrefix("file:///android_asset/")
        return Typeface.createFromAsset(AppContext.app.assets, path)
    }

    private val paints by lazy {
        val faces = PdfFontWeight.entries.associateWith(::face)
        TextKind.entries.associateWith { kind ->
            TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = kind.size.toFloat()
                typeface = faces.getValue(kind.weight)
            }
        }
    }

    fun paint(kind: TextKind): TextPaint = paints.getValue(kind)

    override fun measureText(text: String, kind: TextKind, width: Double): List<String> =
        measuredPdfLines(text, width) { paint(kind).measureText(it).toDouble() }

    override fun lineHeight(kind: TextKind): Double =
        maxOf(super.lineHeight(kind), paint(kind).fontSpacing.toDouble())
}
