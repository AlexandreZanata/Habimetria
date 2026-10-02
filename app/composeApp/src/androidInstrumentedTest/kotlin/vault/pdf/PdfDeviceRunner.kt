package vault.pdf

import android.app.Instrumentation
import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import vault.local.*
import vault.api.AppContext

// Runs only synthetic data in an isolated folder; never opens the user's DB.
class PdfDeviceRunner : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            runBlocking {
                AppContext.app = targetContext.applicationContext
                checkDeviceOrientations()
                checkDeviceQr()
                val dir = File(targetContext.filesDir, "pdf-device-probe").absolutePath
                val fs = FileSystem.SYSTEM
                fs.deleteRecursively(dir.toPath()); fs.createDirectories("$dir/vault".toPath())
                val session = openLocalSession(dir)
                val signer = platformSigner(dir)
                val generator = ReportGenerator(session.db, dir, session.originals, session.derivatives, signer)
                var peakPss = Debug.getPss()
                for ((technical, count) in listOf(false to 100, true to 20)) {
                    val inspection = seedDeviceInspection(session, technical, count)
                    if (technical) ResponsibilityDocuments(dir).attach(session.db, inspection, "$dir/rental.pdf", "SYNTHETIC-PDF-NOT-AN-ART.pdf")
                    LocalSealer(session.db, session.originals, signer).seal(inspection)
                    val started = System.nanoTime()
                    var completedPages = 0
                    val report = generator.generate(inspection, AndroidPlanRenderer(), onProgress = { done, total ->
                        completedPages = total; check(done in 1..total); peakPss = maxOf(peakPss, Debug.getPss())
                    })
                    val pdf = File(generator.pdfPath(report.id))
                    val label = if (technical) "technical" else "rental"
                    pdf.copyTo(File(dir, "$label.pdf"), overwrite = true)
                    check(generator.verify(report.id).allOk())
                    PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { reader ->
                        check(reader.pageCount == completedPages)
                        repeat(reader.pageCount) { page -> reader.openPage(page).use { check(it.width == 595 && it.height == 842) } }
                    }
                    val exported = generator.exportZip(report.id)
                    File(exported).copyTo(File(dir, "$label.zip"), overwrite = true)
                    val imported = importPackageZip(exported, dir)
                    check(PackageVerifier(signer).verify(imported).accepted())
                    fs.deleteRecursively(imported.toPath())
                    val millis = (System.nanoTime() - started) / 1_000_000
                    result.putString(label, "photos=$count pages=$completedPages pdfBytes=${pdf.length()} elapsedMs=$millis")
                }
                result.putString("orientation", "EXIF 1–8 dimensions and all four corners passed")
                result.putString("qr", "100 native bitmap identities decoded with normal detection")
                result.putLong("peakPssKb", peakPss)
                result.putString("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} Android ${android.os.Build.VERSION.RELEASE}")
                result.putString("artifacts", dir)
                session.close()
            }
            finish(Activity.RESULT_OK, result)
        } catch (e: Throwable) {
            result.putString("failure", e.stackTraceToString()); finish(Activity.RESULT_CANCELED, result)
        }
    }
}
