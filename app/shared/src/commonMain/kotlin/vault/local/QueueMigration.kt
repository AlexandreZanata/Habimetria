package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase
import vault.queue.CaptureStore

// QueueMigration imports legacy queue.json rows into the local vault
// without discarding photos. Missing files are reported, never silently
// dropped. One-shot: the queue is cleared and a marker is written so a
// restart cannot duplicate the import.
@Serializable
data class QueueMigrationMarker(
    val inspectionId: String,
    val imported: Int,
    val missingFiles: List<String>,
    val migratedAtDevice: String,
)

data class QueueMigrationReport(
    val inspectionId: String,
    val imported: Int,
    val missingFiles: List<String>,
    val skippedAlreadyMigrated: Boolean = false,
)

class QueueMigration(
    private val fs: FileSystem,
    private val filesDir: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun markerPath(): String = "$filesDir/vault/queue-migrated.json"

    fun alreadyMigrated(): Boolean = fs.exists(markerPath().toPath())

    suspend fun migrate(
        queue: CaptureStore,
        db: VaultDatabase,
        originals: OriginalStore,
        derivatives: DerivativeStore? = null,
    ): QueueMigrationReport {
        if (alreadyMigrated()) {
            val marker = json.decodeFromString<QueueMigrationMarker>(
                fs.read(markerPath().toPath()) { readUtf8() },
            )
            return QueueMigrationReport(marker.inspectionId, 0, emptyList(), true)
        }
        val items = queue.load()
        if (items.isEmpty()) return QueueMigrationReport("", 0, emptyList())

        val repo = LocalVaultRepository(db)
        val property = repo.createProperty("Migrated queue")
        val tenancy = repo.createTenancy(property.id)
        val inspection = repo.createInspection(tenancy.id)
        val roomsByLabel = mutableMapOf<String, String>()
        val missing = mutableListOf<String>()
        var imported = 0

        for (item in items) {
            val source = item.filePath.toPath()
            if (!fs.exists(source)) {
                missing.add(item.localId)
                continue
            }
            val bytes = fs.read(source) { readByteArray() }
            val label = item.roomKey.ifBlank { "Migrated" }
            val roomId = roomsByLabel.getOrPut(label) {
                repo.addRoom(inspection.id, label).id
            }
            val evidenceId = newLocalId("ev")
            val stored = originals.storeOriginal(evidenceId, bytes)
            try {
                derivatives?.ensureDerivatives(evidenceId, bytes)
            } catch (_: Exception) {
                // Thumb failure never loses the photo; views fall back
                // to the original and retry lazily.
            }
            repo.recordEvidence(
                inspectionId = inspection.id,
                roomId = roomId,
                fileName = "$evidenceId.bin",
                mime = item.mime,
                size = stored.size,
                sha256 = stored.sha256,
                origin = "migrated-queue",
                evidenceId = evidenceId,
            )
            imported++
        }

        queue.save(emptyList())
        fs.createDirectories("$filesDir/vault".toPath())
        fs.write(markerPath().toPath()) {
            writeUtf8(
                json.encodeToString(
                    QueueMigrationMarker.serializer(),
                    QueueMigrationMarker(inspection.id, imported, missing, deviceNowIso()),
                ),
            )
        }
        return QueueMigrationReport(inspection.id, imported, missing)
    }
}
