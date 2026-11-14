package vault.account

import okio.FileSystem
import okio.Path

/** Non-secret installation marker only; never a credential fallback. */
internal class SessionInstallation(private val fs: FileSystem, private val marker: Path) {
    fun prepare(erasePrevious: () -> Unit) {
        if (fs.exists(marker)) { validate(); return }
        // Keychain can outlive an app installation. Purge before acknowledging
        // a new installation; a crash here may require login but cannot revive it.
        erasePrevious()
        val staged = marker.parent!! / "installation.pending"
        fs.write(staged) { writeByte(1) }
        fs.atomicMove(staged, marker)
        validate()
    }
    private fun validate() {
        fs.read(marker) {
            if (exhausted() || readByte() != 1.toByte() || !exhausted())
                throw SessionFailure("storage_unavailable")
        }
    }
}
