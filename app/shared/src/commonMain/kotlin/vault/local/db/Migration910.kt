package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `visual_diffs` (`id` TEXT NOT NULL, " +
                "`tenancyId` TEXT NOT NULL, `exitInspectionId` TEXT NOT NULL, " +
                "`entryRoomId` TEXT NOT NULL, `exitRoomId` TEXT NOT NULL, " +
                "`shotCategory` TEXT NOT NULL, `entryEvidenceId` TEXT NOT NULL, " +
                "`exitEvidenceId` TEXT NOT NULL, `signals` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_visual_diffs_exitInspectionId` " +
                "ON `visual_diffs` (`exitInspectionId`)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_visual_diffs_exitRoomId_shotCategory` " +
                "ON `visual_diffs` (`exitRoomId`, `shotCategory`)",
        )
    }
}

// v10 → v11: server-mirror links. Starts empty; existing rows are
// untouched and nothing is re-pushed by migration.
