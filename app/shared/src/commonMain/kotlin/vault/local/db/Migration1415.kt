package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS finding_problems (" +
                "code TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, " +
                "active INTEGER NOT NULL, builtin INTEGER NOT NULL)",
        )
    }
}
