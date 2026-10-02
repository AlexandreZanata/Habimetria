package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `comparisons` (`id` TEXT NOT NULL, " +
                "`tenancyId` TEXT NOT NULL, `entryRoomId` TEXT NOT NULL, " +
                "`exitRoomId` TEXT NOT NULL, `shotCategory` TEXT NOT NULL, " +
                "`entryEvidenceId` TEXT, `exitEvidenceId` TEXT, `tag` TEXT NOT NULL, " +
                "`createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_comparisons_exitRoomId_shotCategory` " +
                "ON `comparisons` (`exitRoomId`, `shotCategory`)",
        )
    }
}

// v8 → v9: V1.4 inspector suggestions. Hints start empty; nothing is
// inferred by migration. Unique (room, evidence, kind, detail) keeps
// repeated inspector runs idempotent.
