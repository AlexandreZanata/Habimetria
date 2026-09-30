package vault.local

import okio.FileSystem
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import vault.queue.CaptureStore
import vault.queue.QueueStatus

// SyncMode selects the production path. LOCAL_ONLY is the L1 default for
// the local journey: no network workers run, no confirmation is fabricated.
// WITH_SERVER_SYNC keeps the optional API integration (upload workers on).
enum class SyncMode {
    LOCAL_ONLY, WITH_SERVER_SYNC
}

// SyncIndicator mirrors server sync only. Local durability is reported
// separately via storedLocally ("Salvo neste dispositivo").
enum class SyncIndicator {
    LOCAL_ONLY, QUEUED, UPLOADING, CONFIRMED, FAILED
}

data class StorageStatus(
    val storedLocally: Boolean,
    val sync: SyncIndicator,
)

fun QueueStatus.toSyncIndicator(): SyncIndicator = when (this) {
    QueueStatus.QUEUED -> SyncIndicator.QUEUED
    QueueStatus.UPLOADING -> SyncIndicator.UPLOADING
    QueueStatus.CONFIRMED -> SyncIndicator.CONFIRMED
    QueueStatus.FAILED -> SyncIndicator.FAILED
}

// Single gate for every network worker (UploadPump, WorkManager, BGTask).
fun uploadsEnabled(mode: SyncMode): Boolean = mode == SyncMode.WITH_SERVER_SYNC

data class SessionStartup(
    val migration: QueueMigrationReport?,
    val reconcile: ReconcileReport,
)

// LocalSession is the local application core: database, repositories,
// originals, navigation progress and the sync policy in one place.
// UI layers receive this instead of calling VaultApi directly.
class LocalSession(
    val db: VaultDatabase,
    val filesDir: String,
    val syncMode: SyncMode,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    val repository = LocalVaultRepository(db)
    val coverage = VaultCoverage(db)
    val originals = OriginalStore(fs, filesDir)
    val navigation = NavigationStore(fs, filesDir)

    val uploadsAllowed: Boolean get() = uploadsEnabled(syncMode)

    // Startup: import any legacy queue, then reconcile files against rows.
    // Works with the API stopped and networking blocked.
    suspend fun startup(): SessionStartup {
        val queue = CaptureStore(fs, "$filesDir/queue")
        val migration = if (queue.load().isNotEmpty()) {
            QueueMigration(fs, filesDir).migrate(queue, db, originals)
        } else {
            null
        }
        return SessionStartup(migration, originals.reconcile(db))
    }

    fun close() {
        db.close()
    }
}

fun openLocalSession(
    filesDir: String,
    syncMode: SyncMode = SyncMode.LOCAL_ONLY,
    fs: FileSystem = FileSystem.SYSTEM,
): LocalSession {
    val db = localDatabaseBuilder("$filesDir/vault/vault.db").build()
    return LocalSession(db, filesDir, syncMode, fs)
}
