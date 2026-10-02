package vault.local

import kotlinx.coroutines.sync.Mutex

// Generation, export, Share declarations and removal share one authority.
internal val reportArtifactLock = Mutex()
