package vault.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/** The Home banner is visible until this installation opts out. */
class HomeBannerPreference(private val fs: FileSystem, filesDir: String) {
    private val directory = "$filesDir/vault".toPath()
    private val hiddenMarker = directory / "home-banner-hidden"

    suspend fun isVisible(): Boolean = withContext(Dispatchers.Default) {
        !fs.exists(hiddenMarker)
    }

    suspend fun setVisible(visible: Boolean): Unit = withContext(Dispatchers.Default) {
        if (visible) {
            fs.delete(hiddenMarker, mustExist = false)
        } else {
            fs.createDirectories(directory)
            fs.write(hiddenMarker) { }
        }
    }
}
