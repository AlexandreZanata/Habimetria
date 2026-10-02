package vault.local

import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.ByteString.Companion.toByteString
import okio.ByteString.Companion.decodeHex
import vault.local.db.LocalReport
import vault.local.db.VaultDatabase

class ReportDeclarations(private val db: VaultDatabase, private val filesDir: String,
    private val signer: InstallationSigner, private val fs: FileSystem = FileSystem.SYSTEM) {
    fun directory(reportId: String): String { require(safeArtifactId(reportId)); return "$filesDir/vault/declarations/$reportId" }

    internal fun initialize(report: LocalReport) {
        val folder = directory(report.id).toPath()
        fs.createDirectories(folder)
        val index = folder.resolve("declarations-index.json")
        if (fs.exists(index)) {
            val previous = declarationJson.decodeFromString<DeclarationIndexFile>(fs.read(index) { readUtf8() })
            val body = declarationJson.decodeFromString<DeclarationIndex>(previous.payload)
            require(body.entries.isEmpty()) { "cannot overwrite declaration history" }
        }
        publishIndex(report, emptyList())
    }

    fun check(report: LocalReport, pinned: Set<String> = emptySet()): List<DeclarationCheck> {
        val publicKey = SignerMaterial(filesDir, fs).key(report.signerKeyId, signer)
        recoverCommittedFiles(report, publicKey)
        return verifyDeclarations(directory(report.id), report.id, requireNotNull(report.pdfSha256),
            report.manifestSha256, signer, publicKey, fs, pinned)
    }

    // The signed index is the commit point. Only exact hash-bound pending files
    // can complete an interrupted move; received packages are never repaired.
    private fun recoverCommittedFiles(report: LocalReport, publicKey: ByteArray) {
        val root = directory(report.id).toPath()
        val path = root.resolve("declarations-index.json")
        require((fs.metadata(path).size ?: 0) in 1..1_048_576L)
        val file = declarationJson.decodeFromString<DeclarationIndexFile>(fs.read(path) { readUtf8() })
        require(signer.verify(publicKey, file.payload.encodeToByteArray(), file.signature.decodeHex().toByteArray()))
        val index = declarationJson.decodeFromString<DeclarationIndex>(file.payload)
        require(index.reportId == report.id && index.pdfSha256 == report.pdfSha256 && index.manifestSha256 == report.manifestSha256 &&
            index.formatVersion == 1 && index.purpose == "local-share-index/v1" && index.entries.size <= 32)
        index.entries.forEach { reference ->
            require(safeArtifactId(reference.id) && reference.sha256.matches(Regex("[0-9a-f]{64}")))
            val destination = root.resolve("declarations/${reference.id}.json")
            val pending = root.resolve("pending/${reference.id}.json")
            if (!fs.exists(destination) && fs.exists(pending)) {
                val meta = fs.metadata(pending)
                require(meta.isRegularFile && meta.symlinkTarget == null && meta.size in 1..262_144L)
                require(fs.read(pending) { readByteArray() }.toByteString().sha256().hex() == reference.sha256)
                fs.createDirectories(destination.parent!!)
                fs.atomicMove(pending, destination)
            }
        }
    }

    suspend fun append(reportId: String, intent: DeclarationIntent, name: String, role: String, note: String,
        acknowledged: Boolean, certificatePath: String? = null, password: CharArray = charArrayOf()): DeclarationCheck = reportArtifactLock.withLock {
        try {
            require(acknowledged) { "explicit declaration intent required" }
            val report = db.reportDao().get(reportId) ?: error("report not found")
            require(report.status == REPORT_READY && report.signerKeyId == signer.keyId) { "report key unavailable" }
            val generator = ReportGenerator(db, filesDir, OriginalStore(fs, filesDir), DerivativeStore(fs, filesDir, platformScaler()), signer, fs)
            require(generator.verify(report.id).allOk()) { "report verification failed" }
            require(ReportArtifacts(filesDir, fs).receiptVersion(report.id) >= 2) { "generate a new report version for declarations" }
            check(report)
            val root = directory(report.id).toPath()
            val indexFile = declarationJson.decodeFromString<DeclarationIndexFile>(fs.read(root.resolve("declarations-index.json")) { readUtf8() })
            val index = declarationJson.decodeFromString<DeclarationIndex>(indexFile.payload)
            require(index.entries.size < 32) { "declaration limit reached" }
            val body = ShareDeclarationBody(newLocalId("share"), report.id, requireNotNull(report.pdfSha256), report.manifestSha256,
                intent, name.trim(), role.trim(), note.trim(), deviceNowIso(), index.entries.lastOrNull()?.sha256.orEmpty()).also { it.validate() }
            val payload = declarationPayload(body)
            require(payload.encodeToByteArray().size <= 40_000) { "declaration payload too large" }
            val proof = certificatePath?.let { personalCertificateSigner()?.sign(it, password, payload.encodeToByteArray()) ?: error("personal signing unavailable") }
            val identity = proof?.let { personalCertificateSigner()?.verify(it, payload.encodeToByteArray()) ?: error("invalid personal signature") }
            val file = if (proof != null) ShareDeclarationFile(payload, requireNotNull(identity).fingerprint, proof.signature, certificate = proof)
                else ShareDeclarationFile(payload, signer.keyId, signer.sign(payload.encodeToByteArray()).toByteString().hex(), signer.publicKeyUncompressed().toByteString().hex())
            val bytes = declarationJson.encodeToString(file).encodeToByteArray()
            require(bytes.size <= 262_144)
            val path = root.resolve("declarations/${body.id}.json")
            require(!fs.exists(path))
            val pending = root.resolve("pending/${body.id}.json")
            ReportArtifacts(filesDir, fs).publish(pending.toString(), bytes)
            try { publishIndex(report, index.entries + DeclarationReference(body.id, bytes.toByteString().sha256().hex())) }
            catch (e: Exception) { fs.delete(pending, mustExist = false); throw e }
            fs.createDirectories(path.parent!!)
            fs.atomicMove(pending, path)
            check(report).last()
        } finally { password.fill('\u0000') }
    }

    private fun publishIndex(report: LocalReport, entries: List<DeclarationReference>) {
        require(signer.keyId == report.signerKeyId)
        val payload = declarationJson.encodeToString(DeclarationIndex(report.id, requireNotNull(report.pdfSha256), report.manifestSha256, entries))
        val index = DeclarationIndexFile(payload, signer.sign(payload.encodeToByteArray()).toByteString().hex())
        ReportArtifacts(filesDir, fs).publish("${directory(report.id)}/declarations-index.json", declarationJson.encodeToString(index).encodeToByteArray())
    }
}
