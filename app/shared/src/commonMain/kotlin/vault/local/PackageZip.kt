package vault.local

import okio.*
import okio.Path.Companion.toPath

// Portable, uncompressed ZIP. Streaming keeps peak memory independent of
// the number of originals. Only exported package names are admitted.
private data class ZipItem(val name: ByteArray, val path: Path, val size: Long, val crc: Int, val offset: Long)
internal const val MAX_PACKAGE_BYTES = 512L * 1024 * 1024
internal const val MAX_PACKAGE_FILES = 1039

fun packageZip(directory: String, destination: String, fs: FileSystem = FileSystem.SYSTEM): String {
    val root = directory.toPath()
    val paths = fs.list(root).filter { fs.metadata(it).isRegularFile } +
        listOf("originals", "responsibility", "declarations").flatMap { name ->
            val folder = root.resolve(name)
            if (fs.exists(folder)) fs.list(folder) else emptyList()
        }
    require(paths.size in 5..MAX_PACKAGE_FILES) { "invalid package file count" }
    val temporary = "$destination.tmp".toPath()
    fs.createDirectories(temporary.parent!!)
    try {
        fs.sink(temporary).buffer().use { sink ->
            var offset = 0L
            val items = paths.sortedBy { it.toString() }.map { path ->
                val name = path.relativeTo(root).toString()
                require(packageEntryAllowed(name) && fs.metadata(path).isRegularFile && fs.metadata(path).symlinkTarget == null) { "invalid package entry" }
                val size = fs.metadata(path).size!!
                require(size in 1..MAX_ORIGINAL_BYTES && offset + size <= MAX_PACKAGE_BYTES) { "package too large" }
                val crc = fs.source(path).buffer().use { source -> crcSource(source, size) }
                val encoded = name.encodeToByteArray()
                val item = ZipItem(encoded, path, size, crc, offset)
                sink.writeIntLe(0x04034b50).writeShortLe(20).writeShortLe(0x800).writeShortLe(0)
                    .writeShortLe(0).writeShortLe(33).writeIntLe(crc).writeIntLe(size.toInt()).writeIntLe(size.toInt())
                    .writeShortLe(encoded.size).writeShortLe(0).write(encoded)
                fs.source(path).use { source -> sink.writeAll(source) }
                offset += 30 + encoded.size + size
                item
            }
            val centralStart = offset
            items.forEach { item ->
                sink.writeIntLe(0x02014b50).writeShortLe(20).writeShortLe(20).writeShortLe(0x800).writeShortLe(0)
                    .writeShortLe(0).writeShortLe(33).writeIntLe(item.crc).writeIntLe(item.size.toInt()).writeIntLe(item.size.toInt())
                    .writeShortLe(item.name.size).writeShortLe(0).writeShortLe(0).writeShortLe(0).writeShortLe(0)
                    .writeIntLe(0).writeIntLe(item.offset.toInt()).write(item.name)
                offset += 46 + item.name.size
            }
            sink.writeIntLe(0x06054b50).writeShortLe(0).writeShortLe(0).writeShortLe(items.size).writeShortLe(items.size)
                .writeIntLe((offset - centralStart).toInt()).writeIntLe(centralStart.toInt()).writeShortLe(0)
        }
        fs.atomicMove(temporary, destination.toPath())
        return destination
    } catch (e: Exception) { fs.delete(temporary, mustExist = false); throw e }
}

internal fun packageEntryAllowed(name: String): Boolean = name in setOf("report.pdf", "receipt.json", "manifest.json", "manifest.sig.json", "signer.json", "declarations-index.json") ||
    name.matches(Regex("originals/[A-Za-z0-9_-]{1,128}\\.bin")) || name.matches(Regex("responsibility/[A-Za-z0-9_-]{1,128}\\.bin")) ||
    name.matches(Regex("declarations/[A-Za-z0-9_-]{1,128}\\.json"))

internal fun crcSource(source: BufferedSource, size: Long): Int {
    var crc = -1
    repeatChunks(source, size) { chunk -> crc = crcBytes(chunk, crc) }
    return crc.inv()
}

private val crcTable = IntArray(256) { index ->
    var value = index
    repeat(8) { value = (value ushr 1) xor (if ((value and 1) != 0) 0xedb88320.toInt() else 0) }
    value
}
internal fun crcBytes(bytes: ByteArray, seed: Int = -1): Int {
    var crc = seed
    bytes.forEach { byte -> crc = (crc ushr 8) xor crcTable[(crc xor byte.toInt()) and 255] }
    return crc
}

internal fun repeatChunks(source: BufferedSource, size: Long, block: (ByteArray) -> Unit) {
    var remaining = size
    while (remaining > 0) {
        val chunk = source.readByteArray(minOf(remaining, 65_536))
        block(chunk)
        remaining -= chunk.size
    }
}
