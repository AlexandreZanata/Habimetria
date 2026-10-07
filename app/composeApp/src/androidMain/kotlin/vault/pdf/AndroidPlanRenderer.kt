package vault.pdf

import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vault.local.*
import java.io.ByteArrayOutputStream

// Font measurement happens before drawing. No hidden continuation pages,
// truncation, guessed rectangles or success with a missing photo.
class AndroidPlanRenderer : PlanRenderer {
    private val fonts = AndroidPdfFonts()
    override fun measureText(text: String, kind: TextKind, width: Double) = fonts.measureText(text, kind, width)
    override fun lineHeight(kind: TextKind) = fonts.lineHeight(kind)

    override suspend fun render(pages: List<RenderPage>, loadPhoto: suspend (String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit, isCancelled: () -> Boolean): ByteArray = withContext(Dispatchers.Default) {
        val document = PdfDocument()
        try {
            for ((index, plan) in pages.withIndex()) {
                if (isCancelled()) throw RenderCancelledException()
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, plan.number).create())
                try {
                    drawPdfChrome(page.canvas, plan, pages.size, fonts)
                    for (item in plan.items) {
                        if (isCancelled()) throw RenderCancelledException()
                        when (item) {
                            is PageItem.Text -> drawPdfText(page.canvas, item, fonts)
                            is PageItem.Photo -> drawPdfPhoto(page.canvas, item, loadPhoto(item.entry.evidenceId))
                            is PageItem.Qr -> drawPdfQr(page.canvas, item)
                            is PageItem.Finding, is PageItem.HashRow -> Unit // semantic audit markers
                        }
                    }
                } finally { document.finishPage(page) }
                onProgress(index + 1, pages.size)
            }
            ByteArrayOutputStream().use { output -> document.writeTo(output); output.toByteArray() }
        } finally { document.close() }
    }
}
