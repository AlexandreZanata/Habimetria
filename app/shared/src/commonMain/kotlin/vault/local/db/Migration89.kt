package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `ai_suggestions` (`id` TEXT NOT NULL, " +
                "`inspectionId` TEXT NOT NULL, `roomId` TEXT NOT NULL, " +
                "`evidenceId` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`detail` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ai_suggestions_inspectionId` " +
                "ON `ai_suggestions` (`inspectionId`)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_suggestions_roomId_evidenceId_kind_detail` " +
                "ON `ai_suggestions` (`roomId`, `evidenceId`, `kind`, `detail`)",
        )
    }
}

// v9 → v10: V1.5 visual diffs. The list starts empty; nothing is
// inferred by migration. Unique (exit room, category) keeps repeated
// diff runs idempotent.
