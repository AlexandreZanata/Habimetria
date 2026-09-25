package vault.rooms

// Coverage index without AI: required shots per room.
data class RoomCoverage(
    val roomKey: String,
    val required: List<String>,
    val captured: Set<String>
) {
    fun percent(): Int {
        if (required.isEmpty()) return 100
        return (captured.size * 100) / required.size
    }
}
