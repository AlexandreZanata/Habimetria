package vault.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import vault.local.*

internal fun drawPdfChrome(canvas: Canvas, page: RenderPage, total: Int, fonts: AndroidPdfFonts) {
    val small = fonts.paint(TextKind.SMALL)
    canvas.drawText("HABIMETRIA | REGISTRO FOTOGRÁFICO", PDF_MARGIN_LEFT_PT.toFloat(), 45f, small)
    canvas.drawLine(PDF_MARGIN_LEFT_PT.toFloat(), 55f, (PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT).toFloat(), 55f, Paint().apply { color = Color.rgb(18, 61, 53); strokeWidth = 1.5f })
    canvas.drawText(page.header, PDF_MARGIN_LEFT_PT.toFloat(), 807f, small)
    canvas.drawText("${page.number} / $total", 495f, 807f, small)
    canvas.drawText("Hora e localização informadas pelo dispositivo", PDF_MARGIN_LEFT_PT.toFloat(), 824f, small)
}

internal fun drawPdfText(canvas: Canvas, item: PageItem.Text, fonts: AndroidPdfFonts) {
    val rect = requireNotNull(item.rect) { "text must be paginated before drawing" }
    val paint = fonts.paint(item.kind)
    canvas.drawText(item.text, rect.x.toFloat(), (rect.y - paint.fontMetrics.ascent).toFloat(), paint)
}

internal fun drawPdfPhoto(canvas: Canvas, item: PageItem.Photo, bytes: ByteArray?, fonts: AndroidPdfFonts) {
    val bitmap = decodeOrientedAndroid(requireNotNull(bytes) { "missing photo: ${item.entry.evidenceId}" }, PREVIEW_MAX_EDGE)
    try {
        canvas.drawBitmap(bitmap, null, item.rect.androidRect(), Paint(Paint.FILTER_BITMAP_FLAG))
    } finally { bitmap.recycle() }
    drawPhotoStamp(canvas, item, fonts)
}

// Location stamp over the photo (bottom-right, white with shadow), mirroring
// field geotag-camera proofs. Drawn on the page only; photo bytes untouched.
private fun drawPhotoStamp(canvas: Canvas, item: PageItem.Photo, fonts: AndroidPdfFonts) {
    val lines = item.entry.stampLines.filter { it.isNotBlank() }
    if (lines.isEmpty()) return
    val rect = item.rect
    val paint = fonts.paint(TextKind.SMALL).let { base ->
        Paint(base).apply {
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
            setShadowLayer(2.5f, 1f, 1f, Color.BLACK)
        }
    }
    val step = (fonts.lineHeight(TextKind.SMALL) + 2.0).toFloat()
    var baseline = (rect.y + rect.h - 4.0).toFloat()
    for (line in lines.asReversed()) {
        canvas.drawText(line, (rect.x + rect.w - 4.0).toFloat(), baseline, paint)
        baseline -= step
    }
}

internal fun drawPdfQr(canvas: Canvas, item: PageItem.Qr) {
    val rect = requireNotNull(item.rect)
    val bitmap = pdfQrBitmap(item.content)
    try {
        canvas.drawBitmap(bitmap, null, rect.androidRect(), null)
    } finally { bitmap.recycle() }
}

internal fun pdfQrBitmap(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 400, 400,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    val pixels = IntArray(400 * 400) { index -> if (matrix[index % 400, index / 400]) Color.BLACK else Color.WHITE }
    return Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565).apply { setPixels(pixels, 0, 400, 0, 0, 400, 400) }
}

private fun PdfRect.androidRect(): RectF = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
