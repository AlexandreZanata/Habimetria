package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString

internal fun verifyDeclarations(directory: String, reportId: String, pdfHash: String, manifestHash: String,
    signer: InstallationSigner, publicKey: ByteArray, fs: FileSystem, pinned: Set<String> = emptySet()): List<DeclarationCheck> {
    val root = directory.toPath()
    fun read(name: String, limit: Long): ByteArray {
        val file = root.resolve(name)
        val meta = fs.metadata(file)
        require(meta.isRegularFile && meta.symlinkTarget == null && meta.size in 1..limit) { "invalid declaration file" }
        require(fs.canonicalize(file).toString().startsWith(fs.canonicalize(root).toString() + "/"))
        return fs.read(file) { readByteArray() }
    }
    val indexFile = declarationJson.decodeFromString<DeclarationIndexFile>(read("declarations-index.json", 1_048_576).decodeToString())
    require(signer.verify(publicKey, indexFile.payload.encodeToByteArray(), indexFile.signature.decodeHex().toByteArray())) { "invalid declaration index signature" }
    val index = declarationJson.decodeFromString<DeclarationIndex>(indexFile.payload)
    require(index.formatVersion == 1 && index.purpose == "local-share-index/v1" && index.reportId == reportId &&
        index.pdfSha256 == pdfHash && index.manifestSha256 == manifestHash) { "declaration index binding mismatch" }
    require(index.entries.size <= 32 && index.entries.distinctBy { it.id }.size == index.entries.size)
    val expected = index.entries.map { "${it.id}.json" }.toSet()
    val folder = root.resolve("declarations")
    require(fs.listOrNull(folder).orEmpty().map { it.name }.toSet() == expected) { "unindexed or missing declarations" }
    var previous = ""
    return index.entries.map { reference ->
        require(safeArtifactId(reference.id) && reference.sha256.matches(Regex("[0-9a-f]{64}")))
        val bytes = read("declarations/${reference.id}.json", 262_144)
        require(bytes.toByteString().sha256().hex() == reference.sha256) { "altered declaration" }
        val file = declarationJson.decodeFromString<ShareDeclarationFile>(bytes.decodeToString())
        require(file.payload.encodeToByteArray().size <= 40_000)
        val body = declarationJson.decodeFromString<ShareDeclarationBody>(file.payload).also { it.validate() }
        require(body.id == reference.id && body.reportId == reportId && body.pdfSha256 == pdfHash &&
            body.manifestSha256 == manifestHash && body.previousSha256 == previous) { "declaration binding mismatch" }
        val certificate = file.certificate?.let { proof ->
            require(file.publicKey.isEmpty() && file.signature == proof.signature)
            val identity = personalCertificateSigner()?.verify(proof, file.payload.encodeToByteArray())
                ?: error("certificate signature verification unavailable or invalid")
            require(identity.fingerprint == file.keyId) { "certificate fingerprint mismatch" }
            identity
        }
        if (certificate == null) {
            val key = file.publicKey.decodeHex().toByteArray()
            require(file.keyId == keyFingerprint(key) && signer.verify(key, file.payload.encodeToByteArray(), file.signature.decodeHex().toByteArray()))
        }
        previous = reference.sha256
        DeclarationCheck(body.intent, body.actorName, body.actorRole, body.note, body.createdAtDevice, true,
            file.keyId, certificate, file.keyId in pinned)
    }
}
