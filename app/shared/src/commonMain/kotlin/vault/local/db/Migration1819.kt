package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Property cover photo for the home card. Organizational only: never
// touches sealed data, existing inspections or the display address line.
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE properties ADD COLUMN photoPath TEXT")
    }
}
