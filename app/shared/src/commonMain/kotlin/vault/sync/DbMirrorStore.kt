package vault.sync

import vault.local.db.MirrorLink
import vault.local.db.VaultDatabase
import vault.local.deviceNowIso

// DbMirrorStore survives force-stop/relaunch: AlreadyMirrored holds
// across restarts, so a retried mirror never duplicates server rows.
// First write wins to match the server's idempotent rows.
class DbMirrorStore(private val db: VaultDatabase) : MirrorStore {
    override suspend fun remoteIdFor(localId: String): String? =
        db.mirrorLinkDao().get(localId)?.remoteId

    override suspend fun record(localId: String, remoteId: String) {
        db.mirrorLinkDao().insert(
            MirrorLink(localId = localId, remoteId = remoteId, createdAtDevice = deviceNowIso()),
        )
    }
}
