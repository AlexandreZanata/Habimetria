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
                val page = document.startPage(PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH_PT.toInt(), PDF_PAGE_HEIGHT_PT.toInt(), plan.number).create())
                try {
                    for (item in plan.items + plan.chrome) {
                        if (isCancelled()) throw RenderCancelledException()
                        when (item) {
                            is PageItem.Fill -> drawPdfFill(page.canvas, item)
                            is PageItem.Path -> drawPdfPath(page.canvas, item)
                            is PageItem.Text -> drawPdfText(page.canvas, item, fonts)
                            is PageItem.Photo -> drawPdfPhoto(page.canvas, item, loadPhoto(item.entry.evidenceId), fonts)
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
