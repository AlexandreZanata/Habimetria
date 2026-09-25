package vault.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import vault.api.AppContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 40f

// Android pipeline: zero extra deps beyond zxing for QR.
class AndroidPlatformPdf : PlatformPdf {

    override fun qr(content: String, sizePx: Int): ByteArray {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    override fun render(doc: PdfDoc, seal: SealBlock): ByteArray {
        val pdf = PdfDocument()
        val title = Paint().apply { textSize = 20f }
        val body = Paint().apply { textSize = 12f }
        val small = Paint().apply { textSize = 10f }

        page(pdf) { canvas ->
            var y = 80f
            canvas.drawText(doc.title, MARGIN, y, title)
            y += 40f
            canvas.drawText("Verify code: ${doc.verifyCode}", MARGIN, y, body)
            y += 24f
            canvas.drawText(doc.verifyUrl.take(72), MARGIN, y, small)
            y += 24f
            canvas.drawText("Sealed (server time): ${doc.sealedAt}", MARGIN, y, small)
        }

        doc.sections.forEach { section ->
            page(pdf) { canvas ->
                var y = 80f
                canvas.drawText(section.label, MARGIN, y, title)
                y += 32f
                section.photos.forEach { photo ->
                    val bmp = scaled(photo.bytes, PAGE_W - 80)
                    if (bmp != null) {
                        val h = bmp.height * ((PAGE_W - 80f) / bmp.width)
                        if (y + h > PAGE_H - 60) return@page
                        canvas.drawBitmap(bmp, null, RectF(MARGIN, y, (PAGE_W - MARGIN), y + h), null)
                        y += h + 8f
                    }
                    canvas.drawText(photo.caption.take(80), MARGIN, y, small)
                    y += 20f
                }
                section.findings.forEach { finding ->
                    if (y > PAGE_H - 60) return@page
                    canvas.drawText("• ${finding.preset} — ${finding.note ?: ""}".take(80), MARGIN, y, small)
                    y += 20f
                }
            }
        }

        page(pdf) { canvas ->
            var y = 80f
            canvas.drawText("Verify this report", MARGIN, y, title)
            y += 32f
            canvas.drawText(doc.verifyUrl.take(72), MARGIN, y, small)
            y += 24f
            canvas.drawText("Manifest: ${doc.manifestSha.take(32)}…", MARGIN, y, small)
            y += 40f
            val qr = BitmapFactory.decodeByteArray(seal.qrPng, 0, seal.qrPng.size)
            if (qr != null) {
                val side = 220f
                canvas.drawBitmap(qr, null, RectF(MARGIN, y, MARGIN + side, y + side), null)
            }
        }

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    override fun share(path: String, mime: String) {
        val context = AppContext.app
        val uri = FileProvider.getUriForFile(context, "vault.app.fileprovider", File(path))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun page(pdf: PdfDocument, draw: (android.graphics.Canvas) -> Unit) {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = pdf.startPage(info)
        draw(page.canvas)
        pdf.finishPage(page)
    }

    private fun scaled(bytes: ByteArray, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
