package vault.ui

// System document selection copies one user-selected file into private
// storage. Callers remove the temporary copy after import/attachment.
data class PickedLocalFile(val path: String, val name: String)
interface LocalFilePicker {
    suspend fun pick(mime: String): PickedLocalFile?
}
