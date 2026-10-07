package vault.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import vault.local.*

internal fun drawPdfChrome(canvas: Canvas, page: RenderPage, total: Int, fonts: AndroidPdfFonts) {
    val small = fonts.paint(TextKind.SMALL)
    canvas.drawText("VISTORIA | REGISTRO FOTOGRÁFICO", PDF_MARGIN_LEFT_PT.toFloat(), 32f, small)
    canvas.drawLine(40f, 43f, 555f, 43f, Paint().apply { color = Color.rgb(22, 101, 52); strokeWidth = 1.5f })
    canvas.drawText(page.header, PDF_MARGIN_LEFT_PT.toFloat(), 807f, small)
    canvas.drawText("${page.number} / $total", 510f, 807f, small)
    canvas.drawText("Hora e localização informadas pelo dispositivo", 40f, 824f, small)
}

internal fun drawPdfText(canvas: Canvas, item: PageItem.Text, fonts: AndroidPdfFonts) {
    val rect = requireNotNull(item.rect) { "text must be paginated before drawing" }
    val paint = fonts.paint(item.kind)
    canvas.drawText(item.text, rect.x.toFloat(), (rect.y - paint.fontMetrics.ascent).toFloat(), paint)
}

internal fun drawPdfPhoto(canvas: Canvas, item: PageItem.Photo, bytes: ByteArray?) {
    val bitmap = decodeOrientedAndroid(requireNotNull(bytes) { "missing photo: ${item.entry.evidenceId}" }, PREVIEW_MAX_EDGE)
    try {
        canvas.drawBitmap(bitmap, null, item.rect.androidRect(), Paint(Paint.FILTER_BITMAP_FLAG))
    } finally { bitmap.recycle() }
}

internal fun drawPdfQr(canvas: Canvas, item: PageItem.Qr) {
    val rect = requireNotNull(item.rect)
    val matrix = QRCodeWriter().encode(item.content, BarcodeFormat.QR_CODE, 400, 400)
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
    try {
        for (x in 0 until 400) for (y in 0 until 400) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        canvas.drawBitmap(bitmap, null, rect.androidRect(), null)
    } finally { bitmap.recycle() }
}

private fun PdfRect.androidRect(): RectF = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
