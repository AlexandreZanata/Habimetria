package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE rooms ADD COLUMN sourceRoomId TEXT")
    }
}

// v7 → v8: entry/exit comparisons. Tags are user-confirmed state;
// no backfill, nothing inferred.
