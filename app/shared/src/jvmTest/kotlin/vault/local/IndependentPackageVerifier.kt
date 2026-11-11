package vault.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// Keep stdout JSON separate from Go toolchain/download/rejection stderr.
internal fun independentPackageVerdict(directory: String): JsonObject {
    var root = File(System.getProperty("user.dir")).absoluteFile
    while (!File(root, "backend/go.mod").isFile) root = root.parentFile ?: error("repository not found")
    val process = ProcessBuilder("go", "run", "./cmd/verify", "-format", "json", directory)
        .directory(File(root, "backend")).start()
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    val outputReader = thread { process.inputStream.bufferedReader().use { stdout.append(it.readText()) } }
    val errorReader = thread { process.errorStream.bufferedReader().use { stderr.append(it.readText()) } }
    try {
        check(process.waitFor(120, TimeUnit.SECONDS)) { "independent verifier timed out" }
        outputReader.join(5_000)
        errorReader.join(5_000)
        check(!outputReader.isAlive && !errorReader.isAlive) { "verifier output incomplete" }
        check(process.exitValue() in 0..1) { "verifier exit=${process.exitValue()} stderr=$stderr" }
        return try { Json.parseToJsonElement(stdout.toString()).jsonObject }
        catch (e: Exception) { error("unparsable verifier output: $stdout stderr=$stderr cause=${e.message}") }
    } finally { if (process.isAlive) process.destroyForcibly() }
}
