package vault.queue

import vault.api.Evidence

// EvidenceRemote is the upload surface the pump needs.
// VaultApi implements it; tests use fakes.
interface EvidenceRemote {
    suspend fun uploadPhoto(
        inspectionId: String,
        roomId: String?,
        bytes: ByteArray,
        mime: String,
    ): Evidence // server ack carries evidence_id
}
