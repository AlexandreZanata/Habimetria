package vault.api

// RoomGroup pairs a room with its sealed evidence lines, in sequence.
// Pure grouping so the PDF renderer and tests share one rule.
data class RoomGroup(
    val room: Room,
    val lines: List<EvidenceLine>,
)

fun groupEvidences(rooms: List<Room>, lines: List<EvidenceLine>): List<RoomGroup> {
    val byRoom = lines.groupBy { it.roomId }
    return rooms.map { room -> RoomGroup(room, byRoom[room.id].orEmpty()) }
}
