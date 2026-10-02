package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `occurrences` (`id` TEXT NOT NULL, " +
                "`propertyId` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT, " +
                "`createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`propertyId`) REFERENCES `properties`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_occurrences_propertyId` " +
                "ON `occurrences` (`propertyId`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `occurrence_attachments` (`id` TEXT NOT NULL, " +
                "`occurrenceId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                "`mime` TEXT NOT NULL, `size` INTEGER NOT NULL, `sha256` TEXT NOT NULL, " +
                "`capturedAtDevice` TEXT NOT NULL, `note` TEXT, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`occurrenceId`) REFERENCES `occurrences`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_occurrence_attachments_occurrenceId` " +
                "ON `occurrence_attachments` (`occurrenceId`)",
        )
    }
}

// v11 → v12: structured address parts on properties. Pure ADD COLUMN;
// legacy rows keep the formatted `address` line with `street` backfilled
// from it, so resume/prefill still shows something.
