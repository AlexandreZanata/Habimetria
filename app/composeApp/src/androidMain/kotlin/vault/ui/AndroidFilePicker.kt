package vault.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import vault.api.AppContext
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation

class AndroidFilePicker : LocalFilePicker {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CancellableContinuation<Uri?>? = null
    fun attach(launcher: ActivityResultLauncher<Array<String>>) { this.launcher = launcher }
    fun onResult(uri: Uri?) { pending?.let { pending = null; if (it.isActive) it.resume(uri) } }
    override suspend fun pick(mime: String, maxBytes: Long): PickedLocalFile? {
        val picker = launcher ?: error("file picker unavailable")
        check(pending == null) { "file picker busy" }
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            pending = continuation
            continuation.invokeOnCancellation { if (pending === continuation) pending = null }
            try { picker.launch(arrayOf(mime)) }
            catch (e: Exception) { pending = null; continuation.resumeWith(Result.failure(e)) }
        } ?: return null
        return withContext(Dispatchers.IO) {
            val resolver = AppContext.app.contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "documento"
            val root = File(AppContext.app.filesDir, "vault/picked").apply { mkdirs() }
            val target = File(root, "${vault.local.newLocalId("document")}.bin")
            try {
                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(65_536); var size = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            size += read
                            require(size <= maxBytes) { "file too large" }
                            output.write(buffer, 0, read)
                        }
                        require(size > 0) { "empty file" }
                    }
                } ?: error("unreadable document")
                PickedLocalFile(target.absolutePath, name)
            } catch (e: Exception) { target.delete(); throw e }
        }
    }
}
