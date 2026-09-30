package vault.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.io.ByteArrayOutputStream

// Android PlanRenderer: draws every shared item with continuation pages,
// headers/footers/page numbers and bounded photo decoding. Photos scale
// into their exact shared rects (aspect preserved upstream); captions
// use the reserved space below each photo.
class AndroidPlanRenderer : PlanRenderer {

    private val title = TextPaint().apply {
        textSize = PDF_TITLE_SIZE.toFloat()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }
    private val heading = TextPaint().apply {
        textSize = PDF_HEADING_SIZE.toFloat()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }
    private val body = TextPaint().apply { textSize = PDF_BODY_SIZE.toFloat(); color = Color.BLACK }
    private val small = TextPaint().apply { textSize = PDF_SMALL_SIZE.toFloat(); color = Color.DKGRAY }
    private val mono = TextPaint().apply {
        textSize = PDF_SMALL_SIZE.toFloat()
        typeface = Typeface.MONOSPACE
        color = Color.BLACK
    }

    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (evidenceId: String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray = withContext(Dispatchers.Default) {
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                if (isCancelled()) throw RenderCancelledException()
                val ctx = PageContext(pdf, page.header, index + 1, pages.size)
                for (item in page.items) {
                    if (isCancelled()) throw RenderCancelledException()
                    when (item) {
                        is PageItem.Photo -> ctx.drawPhoto(item, loadPhoto(item.entry.evidenceId))
                        is PageItem.Text -> ctx.drawTextBlock(item.text, paintFor(item.kind), 8f)
                        is PageItem.Finding -> ctx.drawTextBlock(
                            "• ${item.entry.preset} — ${item.entry.note ?: ""} (${item.entry.evidenceRef})".take(400),
                            body, 4f,
                        )
                        is PageItem.HashRow -> {
                            ctx.drawTextBlock(item.label, small, 0f)
                            ctx.drawTextBlock(item.hex, mono, 6f)
                        }
                        is PageItem.Qr -> ctx.drawQr(item.content)
                    }
                }
                ctx.finish()
                onProgress(index + 1, pages.size)
            }
            val out = ByteArrayOutputStream()
            pdf.writeTo(out)
            out.toByteArray()
        } finally {
            pdf.close()
        }
    }

    private fun paintFor(kind: TextKind): TextPaint = when (kind) {
        TextKind.TITLE -> title
        TextKind.HEADING -> heading
        TextKind.BODY -> body
        TextKind.SMALL -> small
    }

    private inner class PageContext(
        private val pdf: PdfDocument,
        private val header: String,
        private val number: Int,
        private val total: Int,
    ) {
        private var page: PdfDocument.Page = open(number, continued = false)
        private var canvas: Canvas = page.canvas
        private var cursorY = PDF_MARGIN_TOP_PT.toFloat()
        private val bottom = (PDF_PAGE_HEIGHT_PT - PDF_MARGIN_BOTTOM_PT).toFloat()

        private fun open(number: Int, continued: Boolean): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(
                PDF_PAGE_WIDTH_PT.toInt(), PDF_PAGE_HEIGHT_PT.toInt(), number,
            ).create()
            val opened = pdf.startPage(info)
            val suffix = if (continued) " (cont.)" else ""
            opened.canvas.drawText(header.take(48) + suffix, PDF_MARGIN_LEFT_PT.toFloat(), 36f, small)
            opened.canvas.drawText(
                "p. $number/$total",
                (PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT - 60).toFloat(),
                (PDF_PAGE_HEIGHT_PT - 24).toFloat(),
                small,
            )
            return opened
        }

        // Emergency continuation: shared pagination already fits, so this
        // only triggers on font-metric drift. Content never drops.
        private fun ensure(space: Float) {
            if (cursorY + space <= bottom) return
            pdf.finishPage(page)
            page = open(number, continued = true)
            canvas = page.canvas
            cursorY = PDF_MARGIN_TOP_PT.toFloat()
        }

        fun drawTextBlock(text: String, paint: TextPaint, reserved: Float) {
            val layout = wrapped(text, paint)
            ensure(layout.height.toFloat() + reserved)
            canvas.save()
            canvas.translate(PDF_MARGIN_LEFT_PT.toFloat(), cursorY)
            layout.draw(canvas)
            canvas.restore()
            cursorY += layout.height + reserved
        }

        fun drawPhoto(item: PageItem.Photo, bytes: ByteArray?) {
            val rect = item.rect
            // Slots never overlap by construction; honor exact positions.
            if (cursorY > rect.y + 4) cursorY = rect.y.toFloat()
            ensure((rect.h + 30).toFloat())
            val bitmap = bytes?.let { sampled(it, rect.h.toInt()) }
            if (bitmap != null) {
                canvas.drawBitmap(
                    bitmap, null,
                    RectF(
                        rect.x.toFloat(), rect.y.toFloat(),
                        (rect.x + rect.w).toFloat(), (rect.y + rect.h).toFloat(),
                    ),
                    null,
                )
                bitmap.recycle()
            } else {
                canvas.drawText("[foto indisponível]", rect.x.toFloat(), (rect.y + 12).toFloat(), small)
            }
            canvas.drawText(item.entry.caption.take(90), rect.x.toFloat(), (rect.y + rect.h + 11).toFloat(), small)
            item.entry.claimLine?.let {
                canvas.drawText(it.take(110), rect.x.toFloat(), (rect.y + rect.h + 22).toFloat(), small)
            }
            cursorY = (rect.y + rect.h + 30).toFloat()
        }

        fun drawQr(content: String) {
            val side = 140f
            ensure(side + 8f)
            val qr = qrBitmap(content, 280)
            canvas.drawBitmap(
                qr, null,
                RectF(
                    PDF_MARGIN_LEFT_PT.toFloat(), cursorY,
                    PDF_MARGIN_LEFT_PT.toFloat() + side, cursorY + side,
                ),
                null,
            )
            qr.recycle()
            cursorY += side + 8f
        }

        fun finish() {
            pdf.finishPage(page)
        }
    }

    private fun wrapped(text: String, paint: TextPaint): StaticLayout {
        val width = (PDF_PAGE_WIDTH_PT - PDF_MARGIN_LEFT_PT - PDF_MARGIN_RIGHT_PT).toInt()
        return if (Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        }
    }

    private fun sampled(bytes: ByteArray, reqHeightPt: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val req = reqHeightPt.coerceAtLeast(72)
        while (bounds.outHeight / sample > req * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun qrBitmap(content: String, sizePx: Int): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
