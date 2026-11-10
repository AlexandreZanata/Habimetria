package vault.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Preset room names configured in Settings ("Cômodos padrão"). Pure
// device-local convenience: they never create rooms by themselves and
// are copied into DRAFT inspections on explicit user action. Labels
// freeze into sealed rooms when copied; presets stay editable anytime.
@Entity(tableName = "room_presets")
data class LocalRoomPreset(
    @PrimaryKey val id: String,
    val label: String,
    val sort: Int,
)
