package vault.account

import android.app.KeyguardManager
import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private object AndroidSessionGate { val mutex = Mutex() }

/** One app process, app-private no-backup encrypted record, no plaintext fallback. */
class AndroidSessionVault(context: Context, namespace: String = "main") : SessionVault {
    private val app = context.applicationContext
    private val folder = File(app.noBackupFilesDir, "account-$namespace")
    private val file = AtomicFile(File(folder, "session.bin"))
    private val crypto = AndroidSessionCipher("${app.packageName}.account.$namespace.v1", "${app.packageName}|account|$namespace|v1".encodeToByteArray())
    init { require(namespace.matches(Regex("[a-z0-9_-]{1,40}"))) }

    override suspend fun read(): StoredSession? = protected { unlocked(); readRecord() }
    override suspend fun compareAndSet(expected: StoredSession?, updated: StoredSession?): Boolean = protected {
        unlocked()
        if (readRecord() != expected) return@protected false
        if (updated == null) deleteRecord() else writeRecord(updated)
        true
    }
    override suspend fun erase() = protected { deleteRecord(); crypto.eraseKey() }

    private suspend fun <T> protected(block: () -> T): T = withContext(Dispatchers.IO) {
        AndroidSessionGate.mutex.withLock {
            try { block() } catch (e: SessionFailure) { throw e
            } catch (_: Exception) { throw SessionFailure("storage_unavailable") }
        }
    }
    private fun unlocked() {
        if (app.getSystemService(KeyguardManager::class.java).isDeviceLocked) throw SessionFailure("storage_locked")
    }
    private fun readRecord(): StoredSession? {
        if (!file.baseFile.exists() && !File(folder, "session.bin.bak").exists()) return null
        val encrypted = file.openRead().use { input ->
            val bytes = ByteArray(16385)
            var size = 0
            while (size < bytes.size) {
                val count = input.read(bytes, size, bytes.size - size)
                if (count < 0) break
                size += count
            }
            if (size > 16384) throw SessionFailure("storage_unavailable")
            bytes.copyOf(size)
        }
        val clear = crypto.decrypt(encrypted)
        return try { SessionEncoding.decode(clear) } finally { clear.fill(0); encrypted.fill(0) }
    }
    private fun writeRecord(record: StoredSession) {
        if (!folder.isDirectory && !folder.mkdirs()) throw SessionFailure("storage_unavailable")
        val clear = SessionEncoding.encode(record)
        val encrypted = try { crypto.encrypt(clear) } finally { clear.fill(0) }
        val output = file.startWrite()
        try {
            output.write(encrypted); output.fd.sync(); file.finishWrite(output)
            syncFolder()
            if (readRecord() != record) throw SessionFailure("storage_unavailable")
        } catch (e: Exception) { file.failWrite(output); throw e
        } finally { encrypted.fill(0) }
    }
    private fun deleteRecord() {
        if (!folder.exists()) return
        file.delete()
        if (file.baseFile.exists() || File(folder, "session.bin.bak").exists()) throw SessionFailure("storage_unavailable")
        syncFolder()
    }
    private fun syncFolder() {
        val fd = Os.open(folder.absolutePath, OsConstants.O_RDONLY, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    }
}
