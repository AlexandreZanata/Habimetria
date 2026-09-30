package vault.local

import vault.local.db.VaultDatabase
import vault.rooms.RoomCoverage

// Coverage over distinct shot categories per room. Repeated photos of the
// same category never inflate completion; explicit N/A shrinks the target.
class VaultCoverage(private val db: VaultDatabase) {

    suspend fun roomCoverage(roomId: String): RoomCoverage {
        val room = db.roomDao().get(roomId)
            ?: throw LocalValidationException("room not found")
        val applicable = applicableCategories(room.naCategories).map { it.name }
        val captured = db.evidenceDao().listForRoom(roomId)
            .mapNotNull { ShotCategory.parse(it.shotCategory) }
            .map { it.name }
            .filter { it in applicable }
            .toSet()
        return RoomCoverage(roomKey = room.roomKey, required = applicable, captured = captured)
    }

    suspend fun inspectionCoverage(inspectionId: String): Int {
        val rooms = db.roomDao().listForInspection(inspectionId)
        if (rooms.isEmpty()) return 0
        return rooms.map { roomCoverage(it.id).percent() }.average().toInt()
    }

    suspend fun missingCategories(roomId: String): List<ShotCategory> {
        val room = db.roomDao().get(roomId)
            ?: throw LocalValidationException("room not found")
        val coverage = roomCoverage(roomId)
        return applicableCategories(room.naCategories).filter { it.name !in coverage.captured }
    }
}
