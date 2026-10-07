package vault.local

import vault.local.db.LocalRoomPreset
import vault.local.db.VaultDatabase

// Preset room names managed in Settings. Device-local convenience: they
// never create rooms on their own. Copying into a DRAFT inspection is an
// explicit user action in the rooms screen; existing labels (case-
// insensitive) are never duplicated, and sealed inspections refuse.
data class RoomPreset(
    val id: String,
    val label: String,
)

class RoomPresetRepository(private val db: VaultDatabase) {
    suspend fun list(): List<RoomPreset> =
        db.roomPresetDao().listAll().map { RoomPreset(it.id, it.label) }

    suspend fun add(label: String): RoomPreset {
        val clean = validate(label)
        if (db.roomPresetDao().listAll().any { it.label.equals(clean, ignoreCase = true) }) {
            throw LocalValidationException("room preset already exists")
        }
        val row = LocalRoomPreset(
            id = newLocalId("rpreset"),
            label = clean,
            sort = db.roomPresetDao().maxSort() + 1,
        )
        db.roomPresetDao().insert(row)
        return RoomPreset(row.id, row.label)
    }

    suspend fun rename(id: String, label: String): RoomPreset {
        val clean = validate(label)
        if (db.roomPresetDao().listAll().any { it.id != id && it.label.equals(clean, ignoreCase = true) }) {
            throw LocalValidationException("room preset already exists")
        }
        if (db.roomPresetDao().rename(id, clean) == 0) {
            throw LocalValidationException("room preset not found")
        }
        return RoomPreset(id, clean)
    }

    suspend fun delete(id: String) {
        if (db.roomPresetDao().delete(id) == 0) {
            throw LocalValidationException("room preset not found")
        }
    }

    private fun validate(label: String): String {
        val clean = label.trim()
        if (clean.isEmpty()) throw LocalValidationException("room preset label required")
        if (clean.length > MAX_ROOM_LABEL) throw LocalValidationException("label over $MAX_ROOM_LABEL chars")
        return clean
    }
}
