package vault.local

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase

class LocalStoreException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

// ReconcileReport is never silent: missing or mismatched originals are
// listed explicitly so the UI can show an error instead of an empty grid.
data class ReconcileReport(
    val checked: Int,
    val missingFiles: List<String>,
    val sizeMismatches: List<String>,
    val orphanTmps: List<String>,
) {
    fun hasProblems(): Boolean =
        missingFiles.isNotEmpty() || sizeMismatches.isNotEmpty() || orphanTmps.isNotEmpty()
}

// OriginalStore keeps immutable originals under <filesDir>/vault/originals.
// Writes go to .tmp first and rename atomically; a crash between the two
// leaves an orphan .tmp that reconcile() reports on next startup.
class OriginalStore(
    private val fs: FileSystem,
    private val filesDir: String,
) {
    private fun originalsDir(): String = "$filesDir/vault/originals"

    fun originalPath(evidenceId: String): String = "${originalsDir()}/$evidenceId.bin"

    data class StoredOriginal(val path: String, val size: Long, val sha256: String)

    fun storeOriginal(evidenceId: String, bytes: ByteArray): StoredOriginal {
        if (bytes.isEmpty()) throw LocalStoreException("refusing to store empty original")
        fs.createDirectories(originalsDir().toPath())
        val tmp = "${originalsDir()}/$evidenceId.bin.tmp".toPath()
        val final = originalPath(evidenceId).toPath()
        if (fs.exists(final)) throw LocalStoreException("original $evidenceId already stored")
        fs.write(tmp) { write(bytes) }
        fs.atomicMove(tmp, final)
        val sha = bytes.toByteString().sha256().hex()
        return StoredOriginal(final.toString(), bytes.size.toLong(), sha)
    }

    fun readOriginal(evidenceId: String): ByteArray {
        val path = originalPath(evidenceId).toPath()
        if (!fs.exists(path)) throw LocalStoreException("original $evidenceId missing")
        return fs.read(path) { readByteArray() }
    }

    // Reconcile DB rows against files after every startup. Throws nothing;
    // problems are returned for explicit UI surfacing.
    suspend fun reconcile(db: VaultDatabase): ReconcileReport {
        val missing = mutableListOf<String>()
        val mismatched = mutableListOf<String>()
        var checked = 0
        for (inspection in db.inspectionDao().listAll()) {
            for (evidence in db.evidenceDao().listForInspection(inspection.id)) {
                checked++
                val path = originalPath(evidence.id).toPath()
                if (!fs.exists(path)) {
                    missing.add(evidence.id)
                    continue
                }
                val meta = fs.metadataOrNull(path)
                if (meta?.size != evidence.size) mismatched.add(evidence.id)
            }
        }
        val orphans = mutableListOf<String>()
        val dir = originalsDir().toPath()
        if (fs.exists(dir)) {
            for (segment in fs.listOrNull(dir).orEmpty()) {
                if (segment.name.endsWith(".tmp")) orphans.add(segment.name)
            }
        }
        return ReconcileReport(checked, missing, mismatched, orphans)
    }

    fun sha256Of(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
}
