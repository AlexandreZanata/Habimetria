package vault.local

import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.ByteString.Companion.toByteString
import vault.local.db.VaultDatabase

@Serializable
data class ResponsibilityDocument(val id: String, val name: String, val size: Long, val sha256: String)

class ResponsibilityDocuments(private val filesDir: String, private val fs: FileSystem = FileSystem.SYSTEM) {
    fun path(id: String): String {
        require(safeArtifactId(id)) { "invalid document id" }
        return "$filesDir/vault/responsibility/$id.bin"
    }
    fun verified(document: ResponsibilityDocument): ByteArray {
        val file = path(document.id).toPath()
        require(document.size in 1..10L * 1024 * 1024 && fs.metadata(file).size == document.size) { "document missing or size changed" }
        val bytes = fs.read(file) { readByteArray() }
        require(bytes.toByteString().sha256().hex() == document.sha256) { "document digest mismatch" }
        return bytes
    }
    suspend fun attach(db: VaultDatabase, inspectionId: String, source: String, name: String): ReportDetails = localWrite {
        LocalWriteGuard(db).requireDraft(inspectionId)
        val size = fs.metadata(source.toPath()).size ?: error("unreadable document")
        require(size in 1..10L * 1024 * 1024) { "document too large" }
        val bytes = fs.read(source.toPath()) { readByteArray() }
        require(bytes.take(5).toByteArray().decodeToString() == "%PDF-") { "responsibility document must be PDF" }
        val document = ResponsibilityDocument(newLocalId("responsibility"), name, size, bytes.toByteString().sha256().hex())
        val target = path(document.id).toPath()
        fs.createDirectories(target.parent!!)
        val temporary = "$target.tmp".toPath()
        fs.write(temporary) { write(bytes) }; fs.atomicMove(temporary, target)
        val repository = ReportDetailsRepository(db)
        val previous = repository.get(inspectionId)
        val updated = previous.copy(responsibilityDocument = document)
        try {
            repository.saveLocked(inspectionId, updated)
        } catch (e: Exception) { fs.delete(target, mustExist = false); throw e }
        // A cleanup failure must never remove the newly committed attachment.
        previous.responsibilityDocument?.let { fs.delete(path(it.id).toPath(), mustExist = false) }
        updated
    }
}
