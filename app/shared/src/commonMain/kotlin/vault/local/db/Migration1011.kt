package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `mirror_links` (`localId` TEXT NOT NULL, " +
                "`remoteId` TEXT NOT NULL, `createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`localId`))",
        )
    }
}

// v5 → v6: occurrences timeline per property. No backfill; new rows
// only. Sealed inspections are untouched by definition.
