package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE properties ADD COLUMN street TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE properties ADD COLUMN `number` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE properties ADD COLUMN complement TEXT")
        connection.execSQL("ALTER TABLE properties ADD COLUMN neighborhood TEXT NOT NULL DEFAULT ''")
        connection.execSQL("UPDATE properties SET street = address WHERE street = ''")
    }
}

// v12 → v13: property nickname for the home picker. Empty for every
// existing row; the address line stays the display fallback.
