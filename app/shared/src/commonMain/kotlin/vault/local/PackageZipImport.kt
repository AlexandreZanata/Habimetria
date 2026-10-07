package vault.local

import okio.*
import okio.Path.Companion.toPath

// Supports the app's portable ZIP (stored entries). Rejects compression,
// traversal, duplicate names, descriptors and oversized inputs before use.
fun importPackageZip(zipPath: String, filesDir: String, fs: FileSystem = FileSystem.SYSTEM): String {
    val archive = zipPath.toPath()
    require(fs.metadata(archive).size in 1..MAX_PACKAGE_BYTES) { "package too large" }
    val destination = "$filesDir/vault/imports/${newLocalId("package")}".toPath()
    fs.createDirectories(destination)
    try {
        fs.source(archive).buffer().use { source ->
            val names = mutableSetOf<String>()
            var total = 0L
            while (true) {
                val signature = source.readIntLe()
                if (signature == 0x02014b50 || signature == 0x06054b50) break
                require(signature == 0x04034b50) { "invalid ZIP header" }
                require(source.readShortLe().toInt() in 10..20) { "unsupported ZIP version" }
                val flags = source.readShortLe().toInt() and 0xffff
                require(flags == 0 || flags == 0x800) { "unsupported ZIP flags" }
                require(source.readShortLe().toInt() == 0) { "use original app ZIP (compressed ZIP unsupported)" }
                source.skip(4)
                val crc = source.readIntLe()
                val packed = source.readIntLe().toLong() and 0xffffffffL
                val size = source.readIntLe().toLong() and 0xffffffffL
                val nameLength = source.readShortLe().toInt() and 0xffff
                val extraLength = source.readShortLe().toInt() and 0xffff
                require(size == packed && size in 1..MAX_ORIGINAL_BYTES && nameLength in 1..180) { "invalid ZIP entry" }
                total += size
                require(total <= MAX_PACKAGE_BYTES && names.size < MAX_PACKAGE_FILES) { "package too large" }
                val name = source.readUtf8(nameLength.toLong())
                require(packageEntryAllowed(name) && names.add(name)) { "invalid or duplicate ZIP name" }
                source.skip(extraLength.toLong())
                val path = destination.resolve(name)
                fs.createDirectories(path.parent!!)
                var actual = -1
                fs.sink(path).buffer().use { sink ->
                    repeatChunks(source, size) { chunk -> actual = crcBytes(chunk, actual); sink.write(chunk) }
                }
                require(actual.inv() == crc) { "ZIP checksum mismatch" }
            }
            require(names.containsAll(setOf("report.pdf", "manifest.json", "manifest.sig.json", "receipt.json", "signer.json"))) { "incomplete package" }
        }
        return destination.toString()
    } catch (e: Exception) { fs.deleteRecursively(destination); throw e }
}
