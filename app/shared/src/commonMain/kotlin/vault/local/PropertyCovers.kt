package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath

// Durable cover photo for a Property card. Organizational only: one file
// per property, replaced atomically, never part of sealed payloads.
class PropertyCovers(
    private val filesDir: String,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    fun coverPath(propertyId: String): String {
        require(propertyId.isNotBlank()) { "invalid property id" }
        return "$filesDir/vault/property_covers/$propertyId.bin"
    }

    fun saveCover(propertyId: String, bytes: ByteArray): String {
        require(propertyId.isNotBlank()) { "invalid property id" }
        require(bytes.size in 1..10 * 1024 * 1024) { "cover photo too large" }
        val target = coverPath(propertyId).toPath()
        fs.createDirectories(target.parent!!)
        val tmp = "$target.tmp".toPath()
        fs.write(tmp) { write(bytes) }
        fs.atomicMove(tmp, target)
        return target.toString()
    }

    fun deleteCover(propertyId: String) {
        fs.delete(coverPath(propertyId).toPath(), mustExist = false)
    }
}
