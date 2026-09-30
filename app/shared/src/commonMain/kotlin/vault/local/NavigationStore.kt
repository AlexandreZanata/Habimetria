package vault.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

// Work-in-progress navigation (last inspection/room) persists separately
// from domain states, survives process death and restores the journey.
@Serializable
data class NavigationProgress(
    val inspectionId: String? = null,
    val roomId: String? = null,
)

class NavigationStore(
    private val fs: FileSystem,
    private val filesDir: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun file() = "$filesDir/vault/progress.json".toPath()

    fun load(): NavigationProgress {
        if (!fs.exists(file())) return NavigationProgress()
        return try {
            json.decodeFromString<NavigationProgress>(fs.read(file()) { readUtf8() })
        } catch (_: Exception) {
            NavigationProgress()
        }
    }

    fun save(progress: NavigationProgress) {
        fs.createDirectories("$filesDir/vault".toPath())
        val tmp = "$filesDir/vault/progress.json.tmp".toPath()
        fs.write(tmp) { writeUtf8(json.encodeToString(NavigationProgress.serializer(), progress)) }
        fs.atomicMove(tmp, file())
    }

    fun clear() {
        if (fs.exists(file())) fs.delete(file())
    }
}
