package vault.pdf

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import vault.local.manifestSha256
import vault.local.qrContent

// Exercise varied masks with the exact bitmap function used by the PDF.
internal fun checkDeviceQr() {
    repeat(100) { index ->
        val content = qrContent("rep_${index.toString(16).padStart(12, '0')}", manifestSha256("fixture$index"))
        val bitmap = pdfQrBitmap(content)
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)),
                mapOf(DecodeHintType.TRY_HARDER to true)).text
            check(decoded == content) { "QR round trip failed: $index" }
        } finally { bitmap.recycle() }
    }
}
