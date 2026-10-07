package vault.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSURL
import platform.UIKit.*
import platform.darwin.NSObject
import vault.api.appFilesDir
import vault.local.newLocalId
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosFilePicker : LocalFilePicker {
    private var delegate: PickerDelegate? = null
    override suspend fun pick(mime: String, maxBytes: Long): PickedLocalFile? {
        check(delegate == null) { "file picker busy" }
        val controller = UIApplication.sharedApplication.keyWindow?.rootViewController ?: error("window unavailable")
        var presenter = controller
        while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
        val picker = UIDocumentPickerViewController(documentTypes = listOf(if (mime == "application/zip") "public.zip-archive" else "com.adobe.pdf"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
        val url = suspendCancellableCoroutine<NSURL?> { continuation ->
            val current = PickerDelegate(continuation) { delegate = null }
            delegate = current; picker.delegate = current
            continuation.invokeOnCancellation { delegate = null }
            presenter.presentViewController(picker, animated = true, completion = null)
        } ?: return null
        return withContext(Dispatchers.IO) {
            val fs = FileSystem.SYSTEM
            val source = requireNotNull(url.path).toPath()
            require(fs.metadata(source).size in 1..maxBytes) { "file too large" }
            val target = "${appFilesDir()}/vault/picked/${newLocalId("document")}.bin".toPath()
            fs.createDirectories(target.parent!!); fs.copy(source, target)
            PickedLocalFile(target.toString(), url.lastPathComponent ?: "documento")
        }
    }
    private class PickerDelegate(private val continuation: CancellableContinuation<NSURL?>, private val clear: () -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            clear(); if (continuation.isActive) continuation.resume(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
        }
        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            clear(); if (continuation.isActive) continuation.resume(null)
        }
    }
}
