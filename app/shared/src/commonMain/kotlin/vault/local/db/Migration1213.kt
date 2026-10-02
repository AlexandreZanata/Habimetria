package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE properties ADD COLUMN nickname TEXT NOT NULL DEFAULT ''")
    }
}

// v13 → v14: property visibility for U07 removal. Pure ADD COLUMN;
// legacy rows migrate as active (archived = 0). Signed Manifest bytes
// never include this organizational field.
