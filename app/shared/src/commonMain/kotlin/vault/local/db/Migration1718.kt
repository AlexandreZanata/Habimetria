package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Preset room names configured in Settings. Device-local convenience
// only: never touches sealed data or existing inspections.
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS room_presets (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "label TEXT NOT NULL, " +
                "sort INTEGER NOT NULL)",
        )
    }
}
