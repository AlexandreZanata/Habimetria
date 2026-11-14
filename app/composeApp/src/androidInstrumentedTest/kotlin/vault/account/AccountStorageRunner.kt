package vault.account

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.KeyStore
import java.util.UUID

/** Synthetic namespace only: never opens the actual account record or local DB. */
class AccountStorageRunner : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val namespace = "probe_" + UUID.randomUUID().toString().replace("-", "")
        val vault = AndroidSessionVault(targetContext, namespace)
        try {
            runBlocking {
                val record = StoredSession("https://account.example.test", UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), "hr1_" + "A".repeat(43), "2026-12-11T12:00:00Z")
                check(vault.read() == null)
                check(vault.compareAndSet(null, record))
                val restarted = AndroidSessionVault(targetContext, namespace)
                check(restarted.read() == record)
                val file = File(targetContext.noBackupFilesDir, "account-$namespace/session.bin")
                check(!file.readBytes().toString(Charsets.ISO_8859_1).contains(record.refreshToken!!))
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                check(keyStore.getKey("${targetContext.packageName}.account.$namespace.v1", null).encoded == null)
                val pending = record.copy(refreshToken = null)
                val outcomes = coroutineScope {
                    listOf(async { vault.compareAndSet(record, pending) }, async { restarted.compareAndSet(record, pending) }).awaitAll()
                }
                check(outcomes.count { it } == 1)
                check(AndroidSessionVault(targetContext, namespace).read()?.refreshToken == null)
                check(vault.compareAndSet(pending, record))
                val bytes = file.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); file.writeBytes(bytes)
                val corrupt = runCatching { vault.read() }.exceptionOrNull()
                check(corrupt is SessionFailure && corrupt.code == "storage_unavailable")
                vault.erase(); check(vault.read() == null)
                check(vault.compareAndSet(null, record)); check(vault.read() == record)
                vault.erase()
            }
            result.putString("accountStorage", "PASS: non-exportable key, encrypted no-backup record, restart, single CAS, pending rotation, corruption denial, explicit recovery")
            result.putString("device", "${android.os.Build.MODEL} Android ${android.os.Build.VERSION.RELEASE}")
            finish(Activity.RESULT_OK, result)
        } catch (e: Throwable) {
            runCatching { runBlocking { vault.erase() } }
            result.putString("failure", if (e is SessionFailure) e.code else e.javaClass.simpleName)
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
