package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `trusted_issuers` (`keyId` TEXT NOT NULL, " +
                "`label` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                "`addedAtDevice` TEXT NOT NULL, PRIMARY KEY(`keyId`))",
        )
    }
}

// v6 → v7: exit pairing link on rooms. Null for every existing row;
// V1.2 fills it only on newly mirrored exit rooms.
