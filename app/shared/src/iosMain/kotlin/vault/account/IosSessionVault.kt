@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package vault.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

private object IosSessionGate { val mutex = Mutex() }

/** Lazy Keychain operations on a background dispatcher; no shared access group. */
class IosSessionVault(filesDir: String) : SessionVault {
    private val fs = FileSystem.SYSTEM
    private val directory = "$filesDir/account-installation-v1".toPath()
    private val installation = SessionInstallation(fs, directory / "installation")
    private val item = KeychainSessionItem()

    override suspend fun read(): StoredSession? = protected { readRecord() }
    override suspend fun compareAndSet(expected: StoredSession?, updated: StoredSession?): Boolean = protected {
        if (readRecord() != expected) return@protected false
        if (updated == null) item.delete() else {
            val bytes = SessionEncoding.encode(updated)
            try { item.write(bytes) } finally { bytes.fill(0) }
        }
        if (readRecord() != updated) throw SessionFailure("storage_unavailable")
        true
    }
    override suspend fun erase() = protected(recover = true) { item.delete() }

    private suspend fun <T> protected(recover: Boolean = false, block: () -> T): T = withContext(Dispatchers.Default) {
        IosSessionGate.mutex.withLock {
            try {
                fs.createDirectories(directory)
                val url = NSURL.fileURLWithPath(directory.toString())
                if (!url.setResourceValue(NSNumber(bool = true), NSURLIsExcludedFromBackupKey, null))
                    throw SessionFailure("storage_unavailable")
                if (recover) {
                    // Explicit forget can recover a corrupt marker or unreadable record.
                    item.delete()
                    fs.delete(directory / "installation", mustExist = false)
                }
                installation.prepare { item.delete() }
                block()
            } catch (e: SessionFailure) { throw e
            } catch (_: Exception) { throw SessionFailure("storage_unavailable") }
        }
    }
    private fun readRecord(): StoredSession? {
        val bytes = item.read() ?: return null
        return try { SessionEncoding.decode(bytes) } finally { bytes.fill(0) }
    }
}
