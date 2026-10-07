package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE properties ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
    }
}

// v14 → v15: finding_problems for problem customization. New table
// only; built-ins seed lazily on first read, so no data migration.
