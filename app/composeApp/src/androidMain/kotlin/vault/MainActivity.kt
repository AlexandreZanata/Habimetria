package vault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import vault.api.AppContext
import vault.api.appFilesDir
import vault.pdf.AndroidPlatformPdf
import vault.ui.AndroidPhotoCapture
import vault.ui.App
import vault.ui.ServerConfig
import vault.ui.UploadWorker
import vault.ui.createContainer

class MainActivity : ComponentActivity() {
    private val baseUrl = ServerConfig.DEFAULT_BASE_URL

    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* preview shows fallback text when denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.app = applicationContext
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.CAMERA)
        }
        val container = createContainer(
            baseUrl = baseUrl,
            filesDir = appFilesDir(),
            photoCapture = AndroidPhotoCapture(),
            platformPdf = AndroidPlatformPdf(),
            onQueued = { UploadWorker.schedule(applicationContext, baseUrl) },
        )
        UploadWorker.schedule(applicationContext, baseUrl)
        setContent { App(container) }
    }
}
