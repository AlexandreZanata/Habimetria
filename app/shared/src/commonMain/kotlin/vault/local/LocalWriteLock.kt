package vault.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// All in-app draft writes and seals share this lock, so a snapshot cannot
// race capture/reorder/details edits. Raw DAO access is not a public write API.
private val draftWriteMutex = Mutex()
internal suspend fun <T> localWrite(block: suspend () -> T): T = draftWriteMutex.withLock { block() }
